package main;

import ATSPMs.IntersectionConfig;
import ATSPMs.calculateTrafficSignalParameters.*;
import ATSPMs.calculateTrafficSignalParameters;
import loadData.loadSiemensEventData;
import ATSPMs.IntersectionConfig.*;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class MainSiemensATSPMS {
    // ***********Global settings************
    // Database
    public static String hostSiemens="jdbc:mysql://localhost:3306/siemens_database"; // Database name for Siemens' data

    // Users
    public static String userName="root";
    public static String password="!Ganqijian2017";

    // Settings/Variables
    public static Connection conSiemens; // Database connection
    public static int Date=20180303; // Selected date
    public static double FromTime=0*3600; // Beginning time
    public static double ToTime=24*3600;  // Ending time
    public static double Interval=5*60; // Interval of measurements (e.g., flow and occupancy)
    public static int MaxNumPhase=8; // Maximum number of phases
    public static String IntIPStr="172_16_48_145"; // Intersection IP: 2nd Ave and Broad St
    //public static String IntIPStr="172_16_48_140"; // Intersection IP: 2nd Ave and Battery St
    //public static String IntIPStr="172_16_48_158"; // Intersection IP: 2nd Ave and Pike St
    //public static String IntIPStr="172_16_48_164"; // Intersection IP: 2nd Ave and Spring St

	public static String InputFolder="I:\\Dropbox\\WorkAtBerkeley\\Detector_Health_Analysis_Siemens\\Sample_SPM_Data\\2nd_seattle\\seattle_hidef_csv";
    public static String OutputFolder="I:/Dropbox/WorkAtBerkeley/Detector_Health_Analysis_Siemens/Sample_SPM_Data/data_read";
				
    public static void main(final String[] args) {

        try {
            // Get the database connection
            conSiemens = DriverManager.getConnection(hostSiemens, userName, password);
            System.out.println("Succefully connect to the database!");

            // Selection of type of tasks
            int taskID;
            Scanner scanner = new Scanner(System.in);
            System.out.print("Please choose one of the following tasks:\n");
            System.out.print("1:  Load new Siemens event data To DataBase\n"); // Load data
            System.out.print("2:  Generate ATSPMs metric--Split Monitor\n"); //
            System.out.print("3:  Generate ATSPMs metric--Purdue phase termination\n"); //
            System.out.print("4:  Generate ATSPMs metric--Yellow and Red Actuations\n"); //
            System.out.print("5:  Generate ATSPMs metric--Purdue Split Failure\n"); //
            System.out.print("6:  Generate ATSPMs metric--purdue coordination diagram\n"); //
            System.out.print("7:  Generate ATSPMs metric--Approach Flow and Occupancy\n"); //
            System.out.print("8:  Generate ATSPMs metric--Turning Movement Counts\n"); //
            System.out.print("9:  Generate ATSPMs metric--Pedestrian Events\n"); //
            System.out.print("10: Generate ATSPMs metric--Arrival On Red\n"); //
            System.out.print("Please enter your selection (number):");
            taskID =Integer.parseInt(scanner.next());

            if(taskID==1){
                // 1:  Load new Siemens event data To DataBase             
                loadSiemensEventData.mainSiemensRead(conSiemens,InputFolder, OutputFolder);
            }else{
                // Get the intersection properties, list of detectors by approach, and intersection name
                List<IntersectionProperty> intersectionPropertyList= IntersectionConfig.GetIntersectionProperties();
                List<DetectorByApproach> detectorByApproachList=IntersectionConfig.getDetectorByApproachWithGivenIntIPStr(IntIPStr, intersectionPropertyList);
                String IntName=IntersectionConfig.getIntNameWithGivenIntIPStr(IntIPStr,intersectionPropertyList);

                // Generate the performance metric based on the Task ID!
                if(taskID==2){// Draw Split Monitor: for each intersection
                    // Get the planned phase plans
                    List<PlanProperty> planPropertyList=calculateTrafficSignalParameters.getPhasePlanSequenceForGivenDateAndTimePeriod
                            (Date, FromTime, ToTime, IntIPStr, conSiemens);
                    // Get the actual phase activites
                    List<PhasePropertyByID> phasePropertyByIDList=calculateTrafficSignalParameters.getActualPhaseInfForGivenDateAndTimePeriod
                            (Date,FromTime,ToTime,IntIPStr,conSiemens);
                    calculateTrafficSignalParameters.drawSplitMonitor(phasePropertyByIDList,planPropertyList,Date, IntName); // For each intersection
                }else if(taskID==3){
                    // Draw Purdue phase termination
                    List<PlanProperty> planPropertyList=calculateTrafficSignalParameters.getPhasePlanSequenceForGivenDateAndTimePeriod
                            (Date, FromTime, ToTime, IntIPStr, conSiemens);
                    List<PhasePropertyByID> phasePropertyByIDList=calculateTrafficSignalParameters.getActualPhaseInfForGivenDateAndTimePeriod
                            (Date,FromTime,ToTime,IntIPStr,conSiemens);
                    calculateTrafficSignalParameters.drawPurduePhaseTermination(phasePropertyByIDList,planPropertyList,MaxNumPhase,Date, IntName);
                }else if(taskID==4){
                    // Draw Yellow and Red Actuations (only for stopbar detectors)
                    for(int i=0;i<detectorByApproachList.size();i++){// Loop for each approach
                        // Get the stree name, direction, and title
                        String StreetName=detectorByApproachList.get(i).StreetName;
                        String Direction=detectorByApproachList.get(i).ApproachDirection;
                        String Title=IntName+" & "+StreetName+" "+ Direction+ " & "+Date;
                        List<DetectorConfig> detectorConfigList=detectorByApproachList.get(i).detectorConfigList;

                        // Check whether there exist stopbar detectors or not
                        boolean HaveStopbarDetectorLeftTurn=false; // This stands for exclusive left turn
                        List<Integer> DetectorListLeftTurn=new ArrayList<Integer>();
                        boolean HaveStopbardetectorRightTurn=false; // This stands for exclusive right turn
                        List<Integer> DetectorListRightTurn=new ArrayList<Integer>();
                        boolean HaveStopbarDetectorThrough=false; // This stands for through movement is involved
                        List<Integer> DetectorListThrough=new ArrayList<Integer>();
                        for(int j=0;j<detectorConfigList.size();j++){
                            if(detectorConfigList.get(j).Type.equals("VehicleDetector")){
                                if(detectorConfigList.get(j).IsExclusiveLeft && !detectorConfigList.get(j).IsDuplicated){
                                    HaveStopbarDetectorLeftTurn=true;
                                    DetectorListLeftTurn.add(detectorConfigList.get(j).DetectorID);
                                }
                                if(detectorConfigList.get(j).IsThroughInvolved && !detectorConfigList.get(j).IsDuplicated){
                                    HaveStopbarDetectorThrough=true;
                                    DetectorListThrough.add(detectorConfigList.get(j).DetectorID);
                                }
                                if(detectorConfigList.get(j).IsExclusiveRight && !detectorConfigList.get(j).IsDuplicated){
                                    HaveStopbardetectorRightTurn=true;
                                    DetectorListRightTurn.add(detectorConfigList.get(j).DetectorID);
                                }
                            }
                        }
                        if(HaveStopbarDetectorLeftTurn && detectorByApproachList.get(i).PhaseForLeftTurn>0){
                            // Have detectors and the corresponding phase for left turns
                            calculateTrafficSignalParameters.drawYellowAndRedActuations(DetectorListLeftTurn,detectorByApproachList.get(i).PhaseForLeftTurn,
                                    "Left Turn", Date, FromTime, ToTime, IntIPStr, conSiemens, "Yellow and Red Actuation at "+Title, Interval);
                        }
                        if(HaveStopbarDetectorThrough && detectorByApproachList.get(i).PhaseForThroughMovement>0){
                            // Have detectors and the corresponding phase for through movements
                            calculateTrafficSignalParameters.drawYellowAndRedActuations(DetectorListThrough,detectorByApproachList.get(i).PhaseForThroughMovement,
                                    "Through", Date, FromTime, ToTime, IntIPStr, conSiemens, "Yellow and Red Actuation at "+Title, Interval);
                        }
                        if(HaveStopbardetectorRightTurn && detectorByApproachList.get(i).PhaseForRightTurn>0){
                            // Have detectors and the corresponding phase for right turns
                            calculateTrafficSignalParameters.drawYellowAndRedActuations(DetectorListRightTurn,detectorByApproachList.get(i).PhaseForRightTurn,
                                    "Right Turn", Date, FromTime, ToTime, IntIPStr, conSiemens, "Yellow and Red Actuation at "+Title, Interval);
                        }
                    }
                }else if(taskID==5){
                    // Draw Purdue Split Failure
                    for(int i=0;i<detectorByApproachList.size();i++){// Loop for each approach
                        // Get the stree name, direction, and title
                        String StreetName=detectorByApproachList.get(i).StreetName;
                        String Direction=detectorByApproachList.get(i).ApproachDirection;
                        String Title=IntName+" & "+StreetName+" "+ Direction+ " & "+Date;
                        List<DetectorConfig> detectorConfigList=detectorByApproachList.get(i).detectorConfigList;

                        // Check whether there exist stopbar detectors or not
                        boolean HaveStopbarDetectorLeftTurn=false; // This stands for exclusive left turn
                        List<Integer> DetectorListLeftTurn=new ArrayList<Integer>();
                        boolean HaveStopbardetectorRightTurn=false; // This stands for exclusive right turn
                        List<Integer> DetectorListRightTurn=new ArrayList<Integer>();
                        boolean HaveStopbarDetectorThrough=false; // This stands for through movement is involved
                        List<Integer> DetectorListThrough=new ArrayList<Integer>();
                        for(int j=0;j<detectorConfigList.size();j++){
                            if(detectorConfigList.get(j).Type.equals("VehicleDetector")){
                                if(detectorConfigList.get(j).IsExclusiveLeft && !detectorConfigList.get(j).IsDuplicated){
                                    HaveStopbarDetectorLeftTurn=true;
                                    DetectorListLeftTurn.add(detectorConfigList.get(j).DetectorID);
                                }
                                if(detectorConfigList.get(j).IsThroughInvolved && !detectorConfigList.get(j).IsDuplicated){
                                    HaveStopbarDetectorThrough=true;
                                    DetectorListThrough.add(detectorConfigList.get(j).DetectorID);
                                }
                                if(detectorConfigList.get(j).IsExclusiveRight && !detectorConfigList.get(j).IsDuplicated){
                                    HaveStopbardetectorRightTurn=true;
                                    DetectorListRightTurn.add(detectorConfigList.get(j).DetectorID);
                                }
                            }
                        }
                        if(HaveStopbarDetectorLeftTurn && detectorByApproachList.get(i).PhaseForLeftTurn>0){
                            // Have detectors and the corresponding phase for left turns
                            calculateTrafficSignalParameters.drawPurdueSplitFailure(DetectorListLeftTurn,detectorByApproachList.get(i).PhaseForLeftTurn,
                                    "Left Turn", Date, FromTime, ToTime, IntIPStr, conSiemens, "Purdue Split Failure at "+Title, Interval);
                        }
                        if(HaveStopbarDetectorThrough && detectorByApproachList.get(i).PhaseForThroughMovement>0){
                            // Have detectors and the corresponding phase for through movements
                            calculateTrafficSignalParameters.drawPurdueSplitFailure(DetectorListThrough,detectorByApproachList.get(i).PhaseForThroughMovement,
                                    "Through", Date, FromTime, ToTime, IntIPStr, conSiemens, "Purdue Split Failure at "+Title, Interval);
                        }
                        if(HaveStopbardetectorRightTurn && detectorByApproachList.get(i).PhaseForRightTurn>0){
                            // Have detectors and the corresponding phase for right turns
                            calculateTrafficSignalParameters.drawPurdueSplitFailure(DetectorListRightTurn,detectorByApproachList.get(i).PhaseForRightTurn,
                                    "Right Turn", Date, FromTime, ToTime, IntIPStr, conSiemens, "Purdue Split Failure at "+Title, Interval);
                        }
                    }
                }else if(taskID==6){
                    // Draw Purdue Coordination Diagram
                    for(int i=0;i<detectorByApproachList.size();i++){
                        // Loop for each approach
                        // Get the stree name, direction, and title
                        String StreetName=detectorByApproachList.get(i).StreetName;
                        String Direction=detectorByApproachList.get(i).ApproachDirection;
                        String Title="Purdue Coordination Diagram at "+IntName+" & "+StreetName+" "+ Direction+ " & "+Date;
                        List<DetectorConfig> detectorConfigList=detectorByApproachList.get(i).detectorConfigList;
                        // Check whether there exist advance detectors or not
                        boolean HaveAdvanceDetector=false;
                        for(int j=0;j<detectorConfigList.size();j++){
                            if(detectorConfigList.get(j).Type.equals("AdvanceDetector")&&
                                    detectorConfigList.get(j).DistanceFromStopbar>0){
                                HaveAdvanceDetector=true;
                            }
                        }
                        if(HaveAdvanceDetector && detectorByApproachList.get(i).PhaseForThroughMovement>0){
                            // Have advanced detector and the corresponding phase at a given approach
                            // Draw the purdue coordination diagram
                            calculateTrafficSignalParameters.drawPurdueCoordinationDiagramByApproach(detectorByApproachList.get(i)
                                    , Date, FromTime, ToTime, IntIPStr, conSiemens, Title, Interval);
                        }
                    }
                }else if(taskID==7){
                    // Approach flow and occupancy: Only look at advance detectors
                    List<ActuationEventByDetector> actuationEventByDetectorList=calculateTrafficSignalParameters.
                            getDetectorActuationEventsForGivenDateAndTimePeriod(Date,FromTime,ToTime,IntIPStr,conSiemens);
                    for(int i=0;i<detectorByApproachList.size();i++){
                        // Loop for each approach
                        String StreetName=detectorByApproachList.get(i).StreetName;
                        String Direction=detectorByApproachList.get(i).ApproachDirection;
                        String Title=IntName+" & "+StreetName+" "+ Direction+ " & "+Date;
                        List<DetectorConfig> detectorConfigList=detectorByApproachList.get(i).detectorConfigList;
                        calculateTrafficSignalParameters.drawApproachFlowAndOccupancy(detectorConfigList,Interval,Title,actuationEventByDetectorList);
                    }
                }else if(taskID==8){
                    // Turning movement counts
                    List<ActuationEventByDetector> actuationEventByDetectorList=calculateTrafficSignalParameters.
                            getDetectorActuationEventsForGivenDateAndTimePeriod(Date,FromTime,ToTime,IntIPStr,conSiemens);
                    for(int i=0;i<detectorByApproachList.size();i++){
                        // Loop for each approach
                        String StreetName=detectorByApproachList.get(i).StreetName;
                        String Direction=detectorByApproachList.get(i).ApproachDirection;
                        String Title=IntName+" & "+StreetName+" "+ Direction+ " & "+Date;
                        List<DetectorConfig> detectorConfigList=detectorByApproachList.get(i).detectorConfigList;
                        calculateTrafficSignalParameters.drawTurningMovementCountByApproach(detectorConfigList,Interval
                                , "Turning Count at "+Title+" : "+"Left Turn",actuationEventByDetectorList,"LeftTurn");
                    }
                }else if(taskID==9){
                    // Draw pedestrian events
                    int [] PedestrianPhases=IntersectionConfig.getPedestrianPhasesWithGivenIntIPStr(IntIPStr,intersectionPropertyList);
                    if(!PedestrianPhases.equals(null)){
                        for(int i=0;i<PedestrianPhases.length;i++){
                            int PhaseID=PedestrianPhases[i];
                            String Title="Pedestrian Activities at "+IntName+" & "+Date+" & Phase "+PhaseID;
                            calculateTrafficSignalParameters.drawPedestrianCountsAndDelayByPhase(PhaseID,Date, FromTime,ToTime,IntIPStr
                                    , conSiemens, Title,Interval);
                        }
                    }
                }else if(taskID==10){
                    // Draw arrival on red
                    for(int i=0;i<detectorByApproachList.size();i++){
                        // Loop for each approach
                        String StreetName=detectorByApproachList.get(i).StreetName;
                        String Direction=detectorByApproachList.get(i).ApproachDirection;
                        String Title=IntName+" & "+StreetName+" "+ Direction+ " & "+Date;
                        List<DetectorConfig> detectorConfigList=detectorByApproachList.get(i).detectorConfigList;

                        boolean HaveAdvanceDetector=false;
                        for(int j=0;j<detectorConfigList.size();j++){
                            if(detectorConfigList.get(j).Type.equals("AdvanceDetector")&&
                                    detectorConfigList.get(j).DistanceFromStopbar>0){
                                HaveAdvanceDetector=true;
                            }
                        }
                        if(HaveAdvanceDetector && detectorByApproachList.get(i).PhaseForThroughMovement>0){
                            // Have advanced detector and the corresponding phase
                            calculateTrafficSignalParameters.drawArrivalOnRedByApproachAndPhase(detectorByApproachList.get(i)
                                    , Date, FromTime, ToTime, IntIPStr, conSiemens, "Arrival On Red at "+Title, Interval);
                        }
                    }
                }else{
                    System.out.print("Unknown task ID!!");
                    System.exit(-1);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
