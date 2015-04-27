package a4bp.integration;

import java.io.ByteArrayInputStream;
import java.io.UnsupportedEncodingException;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.drools.core.command.runtime.rule.InsertElementsCommand;
import org.drools.simulation.fluent.simulation.SimulationFluent;
import org.eclipse.bpmn2.Definitions;
import org.eclipse.bpmn2.ExtensionAttributeValue;
import org.eclipse.bpmn2.Process;
import org.eclipse.bpmn2.Relationship;
import org.eclipse.bpmn2.RootElement;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.util.FeatureMap;
import org.jbpm.simulation.AggregatedSimulationEvent;
import org.jbpm.simulation.PathFinder;
import org.jbpm.simulation.PathFinderFactory;
import org.jbpm.simulation.SimulationEvent;
import org.jbpm.simulation.SimulationInfo;
import org.jbpm.simulation.SimulationRepository;
import org.jbpm.simulation.SimulationRunner;
import org.jbpm.simulation.converter.JSONPathFormatConverter;
import org.jbpm.simulation.impl.WorkingMemorySimulationRepository;
import org.jbpm.simulation.impl.events.ActivitySimulationEvent;
import org.jbpm.simulation.impl.events.AggregatedActivitySimulationEvent;
import org.jbpm.simulation.impl.events.AggregatedProcessSimulationEvent;
import org.jbpm.simulation.impl.events.EndSimulationEvent;
import org.jbpm.simulation.impl.events.GatewaySimulationEvent;
import org.jbpm.simulation.impl.events.GenericSimulationEvent;
import org.jbpm.simulation.impl.events.HTAggregatedSimulationEvent;
import org.jbpm.simulation.impl.events.HumanTaskActivitySimulationEvent;
import org.jbpm.simulation.impl.events.StartSimulationEvent;
import org.jbpm.simulation.util.BPMN2Utils;
import org.joda.time.DateTime;
import org.joda.time.Interval;
import org.json.JSONArray;
import org.json.JSONObject;

import bpsim.BPSimDataType;
import bpsim.BpsimPackage;
import bpsim.Scenario;

public class SimulationServer {

	private List<SimulationEvent> eventAggregations = new ArrayList<SimulationEvent>();
	private List<Long> eventAggregationsTimes = new ArrayList<Long>();
	private Map<String, Integer> pathInfoMap = null;
	private DateTime simTime = null;

	public static void main(String arg[]){
		SimulationServer ss = new SimulationServer();
		try {
			System.out.println(ss.getPathInfo(arg[0]));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public String showVersion(){
		return "1.0";
	}
	
	
	public String getPathInfo(String bpmnXML) throws Exception {
		runConfiguration();
		PathFinder pfinder = PathFinderFactory.getInstance(bpmnXML);
		JSONObject pathjson = pfinder.findPaths(new JSONPathFormatConverter());
		return pathjson.toString();

	}

	public String runSimulation(String bpmnXML)
			throws Exception {
		runConfiguration();
		String processId = UUID.randomUUID().toString();

		return runSimulation(processId,bpmnXML,1, 1, "seconds");
	}
	
	private void runConfiguration() {
		System.setProperty("jbpm.enable.multi.con", "true");
		System.setProperty("a4bp.debug","true");
	}

	public String runSimulation(String processId, String bpmnXML,
			int numInstances, int intervalInt, String intervalUnit)
			throws Exception {

		try {
			runConfiguration();
			Definitions def = BPMN2Utils
					.getDefinitions(new ByteArrayInputStream(getBytes(bpmnXML)));
			// find the process id
			List<RootElement> rootElements = def.getRootElements();
			for (RootElement root : rootElements) {
				if (root instanceof Process) {
					processId = ((Process) root).getId();
				}
			}

			int baseTimeUnit = 2; // default to minutes
			if (def.getRelationships() != null
					&& def.getRelationships().size() > 0) {
				Relationship relationship = def.getRelationships().get(0);
				for (ExtensionAttributeValue extattrval : relationship
						.getExtensionValues()) {
					FeatureMap extensionElements = extattrval.getValue();
					@SuppressWarnings("unchecked")
					List<BPSimDataType> bpsimExtensions = (List<BPSimDataType>) extensionElements
							.get(BpsimPackage.Literals.DOCUMENT_ROOT__BP_SIM_DATA,
									true);
					if (bpsimExtensions != null && bpsimExtensions.size() > 0) {
						BPSimDataType processAnalysis = bpsimExtensions.get(0);
						if (processAnalysis.getScenario() != null
								&& processAnalysis.getScenario().size() > 0) {
							Scenario simulationScenario = processAnalysis
									.getScenario().get(0);
							baseTimeUnit = simulationScenario
									.getScenarioParameters().getBaseTimeUnit()
									.getValue();
						}
					}
				}

			}

			if (intervalUnit.equals("seconds")) {
				intervalInt = intervalInt * 1000;
			} else if (intervalUnit.equals("minutes")) {
				intervalInt = intervalInt * 1000 * 60;
			} else if (intervalUnit.equals("hours")) {
				intervalInt = intervalInt * 1000 * 60 * 60;
			} else if (intervalUnit.equals("days")) {
				intervalInt = intervalInt * 1000 * 60 * 60 * 24;
			} else {
				// default to milliseconds
			}
			
			this.eventAggregations = new ArrayList<SimulationEvent>();
			this.simTime = new DateTime();
			
			SimulationRepository repo = SimulationRunner.runSimulation(
					processId, bpmnXML, numInstances, intervalInt, true,
					"onevent.simulation.rules.drl");
			WorkingMemorySimulationRepository wmRepo = (WorkingMemorySimulationRepository) repo;
			// start evaluating all the simulation events generated
			// wmRepo.fireAllRules();
			List<SimulationEvent> allEvents = new ArrayList<SimulationEvent>(
					wmRepo.getEvents());
			wmRepo.getSession().execute(
					new InsertElementsCommand((Collection) wmRepo
							.getAggregatedEvents()));
			wmRepo.fireAllRules();
			List<AggregatedSimulationEvent> aggEvents = (List<AggregatedSimulationEvent>) wmRepo
					.getGlobal("summary");
			SimulationInfo simInfo = wmRepo.getSimulationInfo();
			wmRepo.close();

			Map<String, Double> numInstanceData = new HashMap<String, Double>();
			JSONObject parentJSON = new JSONObject();
			JSONArray simInfoJSONArray = new JSONArray();
			JSONArray aggProcessSimulationJSONArray = new JSONArray();
			JSONArray aggNumActivityInstancesJSONArray = new JSONArray();
			JSONArray aggHTSimulationJSONArray = new JSONArray();
			JSONArray aggTaskSimulationJSONArray = new JSONArray();

			JSONObject simInfoKeys = new JSONObject();
			simInfoKeys.put(
					"id",
					simInfo.getProcessId() == null ? "" : simInfo
							.getProcessId());
			simInfoKeys.put("name", simInfo.getProcessName() == null ? ""
					: simInfo.getProcessName());
			simInfoKeys.put("executions", simInfo.getNumberOfExecutions());
			SimpleDateFormat infoDateFormat = new SimpleDateFormat(
					"EEE, d MMM yyyy HH:mm:ss");
			String simStartStr = infoDateFormat.format(new Date(simInfo
					.getStartTime()));
			String simEndStr = infoDateFormat.format(new Date(simInfo
					.getEndTime()));
			simInfoKeys.put("starttime", simStartStr);
			simInfoKeys.put("endtime", simEndStr);
			simInfoKeys.put("version", simInfo.getProcessVersion() == null ? ""
					: simInfo.getProcessVersion());
			simInfoKeys.put("interval",
					presentInterval((int) simInfo.getInterval(), intervalUnit));
			simInfoJSONArray.put(simInfoKeys);

			for (AggregatedSimulationEvent aggEvent : aggEvents) {
				if (aggEvent instanceof AggregatedProcessSimulationEvent) {
					AggregatedProcessSimulationEvent event = (AggregatedProcessSimulationEvent) aggEvent;
					JSONObject processSimKeys = new JSONObject();
					processSimKeys.put("key", "Process Avarages");
					processSimKeys.put("id", event.getProcessId());
					processSimKeys.put("name", event.getProcessName());
					JSONArray processSimValues = new JSONArray();
					JSONObject obj1 = new JSONObject();
					obj1.put("label", "Max Execution Time");
					obj1.put(
							"value",
							adjustToBaseTimeUnit(event.getMaxExecutionTime(),
									baseTimeUnit));
					JSONObject obj2 = new JSONObject();
					obj2.put("label", "Min Execution Time");
					obj2.put(
							"value",
							adjustToBaseTimeUnit(event.getMinExecutionTime(),
									baseTimeUnit));
					JSONObject obj3 = new JSONObject();
					obj3.put("label", "Avg. Execution Time");
					obj3.put(
							"value",
							adjustToBaseTimeUnit(event.getAvgExecutionTime(),
									baseTimeUnit));
					processSimValues.put(obj1);
					processSimValues.put(obj2);
					processSimValues.put(obj3);
					processSimKeys.put("values", processSimValues);
					aggProcessSimulationJSONArray.put(processSimKeys);
					// process paths
					this.pathInfoMap = event.getPathNumberOfInstances();
				} else if (aggEvent instanceof HTAggregatedSimulationEvent) {
					HTAggregatedSimulationEvent event = (HTAggregatedSimulationEvent) aggEvent;
					numInstanceData.put(event.getActivityName(),
							new Long(event.getNumberOfInstances())
									.doubleValue());
					JSONObject allValues = new JSONObject();
					JSONObject resourceValues = new JSONObject();
					JSONObject costValues = new JSONObject();

					allValues.put("key", "Human Task Avarages");
					allValues.put("id", event.getActivityId());
					allValues.put("name", event.getActivityName());

					JSONArray innerExecutionValues = new JSONArray();
					JSONObject obj1 = new JSONObject();
					obj1.put("label", "Max");
					obj1.put(
							"value",
							adjustToBaseTimeUnit(event.getMaxExecutionTime(),
									baseTimeUnit));
					JSONObject obj2 = new JSONObject();
					obj2.put("label", "Min");
					obj2.put(
							"value",
							adjustToBaseTimeUnit(event.getMinExecutionTime(),
									baseTimeUnit));
					JSONObject obj3 = new JSONObject();
					obj3.put("label", "Average");
					obj3.put(
							"value",
							adjustToBaseTimeUnit(event.getAvgExecutionTime(),
									baseTimeUnit));
					innerExecutionValues.put(obj1);
					innerExecutionValues.put(obj2);
					innerExecutionValues.put(obj3);
					JSONObject valuesObj = new JSONObject();
					valuesObj.put("key", "Execution Times");
					valuesObj.put("color", "#1f77b4");
					valuesObj.put("values", innerExecutionValues);

					JSONArray innerExecutionValues2 = new JSONArray();
					JSONObject obj4 = new JSONObject();
					obj4.put("label", "Max");
					obj4.put(
							"value",
							adjustToBaseTimeUnit(event.getMaxWaitTime(),
									baseTimeUnit));
					JSONObject obj5 = new JSONObject();
					obj5.put("label", "Min");
					obj5.put(
							"value",
							adjustToBaseTimeUnit(event.getMinWaitTime(),
									baseTimeUnit));
					JSONObject obj6 = new JSONObject();
					obj6.put("label", "Average");
					obj6.put(
							"value",
							adjustToBaseTimeUnit(event.getAvgWaitTime(),
									baseTimeUnit));
					innerExecutionValues2.put(obj4);
					innerExecutionValues2.put(obj5);
					innerExecutionValues2.put(obj6);
					JSONObject valuesObj2 = new JSONObject();
					valuesObj2.put("key", "Wait Times");
					valuesObj2.put("color", "#d62728");
					valuesObj2.put("values", innerExecutionValues2);

					JSONArray timeValuesInner = new JSONArray();
					timeValuesInner.put(valuesObj);
					timeValuesInner.put(valuesObj2);
					allValues.put("timevalues", timeValuesInner);

					resourceValues.put("key", "Resource Allocations");
					resourceValues.put("id", event.getActivityId());
					resourceValues.put("name", event.getActivityName());
					JSONArray htSimValues2 = new JSONArray();
					JSONObject obj7 = new JSONObject();
					obj7.put("label", "Max");
					obj7.put("value",
							adjustDouble(event.getMaxResourceUtilization()));
					JSONObject obj8 = new JSONObject();
					obj8.put("label", "Min");
					obj8.put("value",
							adjustDouble(event.getMinResourceUtilization()));
					JSONObject obj9 = new JSONObject();
					obj9.put("label", "Average");
					obj9.put("value",
							adjustDouble(event.getAvgResourceUtilization()));
					htSimValues2.put(obj7);
					htSimValues2.put(obj8);
					htSimValues2.put(obj9);
					resourceValues.put("values", htSimValues2);
					allValues.put("resourcevalues", resourceValues);

					costValues.put("key", "Resource Cost");
					costValues.put("id", event.getActivityId());
					costValues.put("name", event.getActivityName());
					JSONArray htSimValues3 = new JSONArray();
					JSONObject obj10 = new JSONObject();
					obj10.put("label", "Max");
					obj10.put("value", adjustDouble(event.getMaxResourceCost()));
					JSONObject obj11 = new JSONObject();
					obj11.put("label", "Min");
					obj11.put("value", adjustDouble(event.getMinResourceCost()));
					JSONObject obj12 = new JSONObject();
					obj12.put("label", "Average");
					obj12.put("value", adjustDouble(event.getAvgResourceCost()));
					htSimValues3.put(obj10);
					htSimValues3.put(obj11);
					htSimValues3.put(obj12);
					costValues.put("values", htSimValues3);
					allValues.put("costvalues", costValues);

					// single events
					// JSONObject taskEvents = getTaskEventsFromAllEvents(event,
					// allEvents);
					// if(taskEvents != null) {
					// allValues.put("timeline", taskEvents);
					// aggHTSimulationJSONArray.put(allValues);
					// }
					aggHTSimulationJSONArray.put(allValues);

				} else if (aggEvent instanceof AggregatedActivitySimulationEvent) {
					AggregatedActivitySimulationEvent event = (AggregatedActivitySimulationEvent) aggEvent;
					numInstanceData.put(event.getActivityName(),
							new Long(event.getNumberOfInstances())
									.doubleValue());

					JSONObject taskSimKeys = new JSONObject();
					taskSimKeys.put("key", "Task Avarages");
					taskSimKeys.put("id", event.getActivityId());
					taskSimKeys.put("name", event.getActivityName());
					JSONArray taskSimValues = new JSONArray();
					JSONObject obj1 = new JSONObject();
					obj1.put("label", "Max. Execution Time");
					obj1.put(
							"value",
							adjustToBaseTimeUnit(event.getMaxExecutionTime(),
									baseTimeUnit));
					JSONObject obj2 = new JSONObject();
					obj2.put("label", "Min. Execution Time");
					obj2.put(
							"value",
							adjustToBaseTimeUnit(event.getMinExecutionTime(),
									baseTimeUnit));
					JSONObject obj3 = new JSONObject();
					obj3.put("label", "Avg. Execution Time");
					obj3.put(
							"value",
							adjustToBaseTimeUnit(event.getAvgExecutionTime(),
									baseTimeUnit));
					taskSimValues.put(obj1);
					taskSimValues.put(obj2);
					taskSimValues.put(obj3);
					taskSimKeys.put("values", taskSimValues);
					// single events
					// JSONObject taskEvents = getTaskEventsFromAllEvents(event,
					// allEvents);
					// if(taskEvents != null) {
					// taskSimKeys.put("timeline", taskEvents);
					// }
					aggTaskSimulationJSONArray.put(taskSimKeys);
				}
			}

			JSONObject numInstancesSimKeys = new JSONObject();
			numInstancesSimKeys.put("key", "Activity Instances");
			numInstancesSimKeys.put("id", "Activity Instances");
			numInstancesSimKeys.put("name", "Activity Instances");
			JSONArray numInstancesValues = new JSONArray();
			Iterator<String> iter = numInstanceData.keySet().iterator();
			while (iter.hasNext()) {
				String key = iter.next();
				Double value = numInstanceData.get(key);
				JSONObject entryObject = new JSONObject();
				entryObject.put("label", key);
				entryObject.put("value", value);
				numInstancesValues.put(entryObject);
			}
			numInstancesSimKeys.put("values", numInstancesValues);
			aggNumActivityInstancesJSONArray.put(numInstancesSimKeys);

			parentJSON.put("siminfo", simInfoJSONArray);
			parentJSON.put("processsim", aggProcessSimulationJSONArray);
			parentJSON.put("activityinstances",
					aggNumActivityInstancesJSONArray);
			parentJSON.put("htsim", aggHTSimulationJSONArray);
			parentJSON.put("tasksim", aggTaskSimulationJSONArray);
			parentJSON.put("timeline",
					getTaskEventsFromAllEvents(null, allEvents, intervalUnit));
			// event aggregations
			JSONArray aggEventProcessSimulationJSONArray = new JSONArray();
			int c = 0;
			for (SimulationEvent simEve : this.eventAggregations) {
				AggregatedProcessSimulationEvent aggProcessEve = (AggregatedProcessSimulationEvent) (((GenericSimulationEvent) simEve)
						.getAggregatedEvent());
				if (aggProcessEve != null) {
					JSONObject eventProcessSimKeys = new JSONObject();
					eventProcessSimKeys.put("key", "Process Avarages");
					eventProcessSimKeys.put("id", aggProcessEve.getProcessId());
					eventProcessSimKeys.put("name",
							aggProcessEve.getProcessName());
					eventProcessSimKeys.put("timesincestart",
							this.eventAggregationsTimes.get(c));
					eventProcessSimKeys.put("timeunit", intervalUnit);
					JSONArray eventProcessSimValues = new JSONArray();
					JSONObject obj1 = new JSONObject();
					obj1.put("label", "Max Execution Time");
					obj1.put(
							"value",
							adjustToBaseTimeUnit(
									aggProcessEve.getMaxExecutionTime(),
									baseTimeUnit));
					JSONObject obj2 = new JSONObject();
					obj2.put("label", "Min Execution Time");
					obj2.put(
							"value",
							adjustToBaseTimeUnit(
									aggProcessEve.getMinExecutionTime(),
									baseTimeUnit));
					JSONObject obj3 = new JSONObject();
					obj3.put("label", "Avg. Execution Time");
					obj3.put(
							"value",
							adjustToBaseTimeUnit(
									aggProcessEve.getAvgExecutionTime(),
									baseTimeUnit));
					eventProcessSimValues.put(obj1);
					eventProcessSimValues.put(obj2);
					eventProcessSimValues.put(obj3);
					eventProcessSimKeys.put("values", eventProcessSimValues);
					aggEventProcessSimulationJSONArray.put(eventProcessSimKeys);
					c++;
				}
			}
			parentJSON.put("eventaggregations",
					aggEventProcessSimulationJSONArray);
			// process paths
			JSONArray processPathsJSONArray = new JSONArray();
			if (this.pathInfoMap != null) {
				Iterator<String> pathKeys = this.pathInfoMap.keySet()
						.iterator();
				while (pathKeys.hasNext()) {
					JSONObject pathsSimKeys = new JSONObject();
					String pkey = pathKeys.next();
					Integer pvalue = this.pathInfoMap.get(pkey);
					pathsSimKeys.put("id", pkey);
					pathsSimKeys.put("numinstances", pvalue);
					pathsSimKeys.put("totalinstances", numInstances);
					processPathsJSONArray.put(pathsSimKeys);
				}
				parentJSON.put("pathsim", processPathsJSONArray);
			}

			return parentJSON.toString();

		} catch (Exception e) {
			return e.getMessage();
		}
	}

	private String presentInterval(int interval, String intervalUnit) {
		String retVal;
		if (intervalUnit.equals("seconds")) {
			interval = interval / 1000;
			retVal = interval + " seconds";
		} else if (intervalUnit.equals("minutes")) {
			interval = interval / (1000 * 60);
			retVal = interval + " minutes";
		} else if (intervalUnit.equals("hours")) {
			interval = interval / (1000 * 60 * 60);
			retVal = interval + " hours";
		} else if (intervalUnit.equals("days")) {
			interval = interval / (1000 * 60 * 60 * 24);
			retVal = interval + " days";
		} else {
			retVal = interval + " milliseconds";
		}
		return retVal;
	}

	private static byte[] getBytes(String string) {
		try {
			return string.getBytes("UTF-8");
		} catch (UnsupportedEncodingException e) {
			throw new IllegalArgumentException(e.getMessage());
		}
	}

	private double adjustToBaseTimeUnit(double in, int baseTime)
			throws ParseException {
		if (in > 0) {
			if (baseTime == 1) {
				in = in / 1000;
			} else if (baseTime == 2) {
				in = in / (1000 * 60);
			} else if (baseTime == 3) {
				in = in / (1000 * 60 * 60);
			} else if (baseTime == 4) {
				in = in / (1000 * 60 * 60 * 24);
			} else if (baseTime == 5) {
				in = in / (1000 * 60 * 60 * 24 * 365);
			}
		}

		DecimalFormat twoDForm = new DecimalFormat("#.##");
		String formattedValue = twoDForm.format(in);
		return twoDForm.parse(formattedValue).doubleValue();
	}

	private double adjustDouble(double in) throws ParseException {
		DecimalFormat twoDForm = new DecimalFormat("#.##");
		String formattedValue = twoDForm.format(in);
		return twoDForm.parse(formattedValue).doubleValue();
	}

	private JSONObject getTaskEventsFromAllEvents(
			AggregatedSimulationEvent event, List<SimulationEvent> allEvents,
			String intervalUnit) throws Exception {
		JSONObject allEventsObject = new JSONObject();
		allEventsObject.put("headline", "Simulation Events");
		allEventsObject.put("type", "default");
		allEventsObject.put("text", "Simulation Events");
		JSONArray allEventsDataArray = new JSONArray();
		for (SimulationEvent se : allEvents) {
			// for now only include end and activity events
			if ((se instanceof EndSimulationEvent)
					|| (se instanceof ActivitySimulationEvent)
					|| (se instanceof HumanTaskActivitySimulationEvent)) {
				if (event != null) {
					String seActivityId = getSingleEventActivityId(se);
					String eventActivitytId = getAggregatedEventActivityId(event);
					if (eventActivitytId.equals(seActivityId)) {
						allEventsDataArray.put(getTimelineEventObject(se,
								intervalUnit));
					}
				} else {
					allEventsDataArray.put(getTimelineEventObject(se,
							intervalUnit));
				}
			}
		}
		allEventsObject.put("date", allEventsDataArray);
		// sort the time values
		Collections.sort(this.eventAggregationsTimes);
		return allEventsObject;
	}

	private String getSingleEventActivityId(SimulationEvent event) {
		if (event != null) {
			if (event instanceof ActivitySimulationEvent) {
				return ((ActivitySimulationEvent) event).getActivityId();
			} else if (event instanceof EndSimulationEvent) {
				return ((EndSimulationEvent) event).getActivityId();
			} else if (event instanceof GatewaySimulationEvent) {
				return ((GatewaySimulationEvent) event).getActivityId();
			} else if (event instanceof HumanTaskActivitySimulationEvent) {
				return ((HumanTaskActivitySimulationEvent) event)
						.getActivityId();
			} else if (event instanceof StartSimulationEvent) {
				return ((StartSimulationEvent) event).getActivityId();
			} else {
				return "";
			}
		} else {
			return "";
		}
	}

	private String getAggregatedEventActivityId(AggregatedSimulationEvent event) {
		if (event instanceof AggregatedProcessSimulationEvent) {
			return ((AggregatedProcessSimulationEvent) event).getProcessId();
		} else if (event instanceof HTAggregatedSimulationEvent) {
			return ((HTAggregatedSimulationEvent) event).getActivityId();
		} else if (event instanceof AggregatedActivitySimulationEvent) {
			return ((AggregatedActivitySimulationEvent) event).getActivityId();
		} else {
			return "";
		}
	}

	private JSONObject getTimelineEventObject(SimulationEvent se,
			String intervalUnit) throws Exception {
		JSONObject seObject = new JSONObject();
		seObject.put("id", se.getUUID().toString());
		seObject.put("startDate", getDateString(se.getStartTime()));
		seObject.put("endDate", getDateString(se.getEndTime()));
		if (se instanceof EndSimulationEvent) {
			seObject.put("headline",
					((EndSimulationEvent) se).getActivityName());
			seObject.put("activityid",
					((EndSimulationEvent) se).getActivityId());
		} else if (se instanceof ActivitySimulationEvent) {
			seObject.put("headline",
					((ActivitySimulationEvent) se).getActivityName());
			seObject.put("activityid",
					((ActivitySimulationEvent) se).getActivityId());
		} else if (se instanceof HumanTaskActivitySimulationEvent) {
			seObject.put("headline",
					((HumanTaskActivitySimulationEvent) se).getActivityName());
			seObject.put("activityid",
					((HumanTaskActivitySimulationEvent) se).getActivityId());
		}
		seObject.put("text", "");
		seObject.put("tag", "");
		JSONObject seAsset = new JSONObject();
		seAsset.put("media", "");
		seAsset.put("thumbnail", getIcon(se));
		seAsset.put("credit", "");
		seAsset.put("caption", "");
		seObject.put("asset", seAsset);

		// add aggregated events as well
		this.eventAggregations.add(se);
		Interval eventinterval = new Interval(this.simTime.getMillis(),
				se.getEndTime());

		long durationvalue = eventinterval.toDurationMillis();
		if (intervalUnit.equals("seconds")) {
			durationvalue = durationvalue / 1000;
		} else if (intervalUnit.equals("minutes")) {
			durationvalue = durationvalue / (1000 * 60);
		} else if (intervalUnit.equals("hours")) {
			durationvalue = durationvalue / (1000 * 60 * 60);
		} else if (intervalUnit.equals("days")) {
			durationvalue = durationvalue / (1000 * 60 * 60 * 24);
		} else {
			// default to milliseconds
		}

		this.eventAggregationsTimes.add(durationvalue);
		return seObject;
	}

	private String getIcon(SimulationEvent se) {
		String contextPath = "";
		if (se != null) {
			if (se instanceof ActivitySimulationEvent) {
				return contextPath
						+ "/org.jbpm.designer.jBPMDesigner/images/simulation/timeline/activity.png";
			} else if (se instanceof EndSimulationEvent) {
				return contextPath
						+ "/org.jbpm.designer.jBPMDesigner/images/simulation/timeline/endevent.png";
			} else if (se instanceof GatewaySimulationEvent) {
				return contextPath
						+ "/org.jbpm.designer.jBPMDesigner/images/simulation/timeline/gateway.png";
			} else if (se instanceof HumanTaskActivitySimulationEvent) {
				return contextPath
						+ "/org.jbpm.designer.jBPMDesigner/images/simulation/timeline/humantask.png";
			} else if (se instanceof StartSimulationEvent) {
				return contextPath
						+ "/org.jbpm.designer.jBPMDesigner/images/simulation/timeline/startevent.png";
			} else {
				return "";
			}
		} else {
			return "";
		}
	}

	private String getDateString(long seDate) {
		Date d = new Date(seDate);
		DateTime dt = new DateTime(seDate);
		StringBuffer retBuf = new StringBuffer();
		retBuf.append(dt.getYear()).append(",");
		retBuf.append(dt.getMonthOfYear()).append(",");
		retBuf.append(dt.getDayOfMonth()).append(",");
		retBuf.append(dt.getHourOfDay()).append(",");
		retBuf.append(dt.getMinuteOfHour()).append(",");
		retBuf.append(dt.getSecondOfMinute()).append(",");
		retBuf.append(dt.getMillisOfSecond());
		return retBuf.toString();
	}
}


