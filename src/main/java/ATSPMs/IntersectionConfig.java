package ATSPMs;

import java.util.*;
import java.util.List;

public class IntersectionConfig {
    // This is a class that specifies the intersection configuration.
    // It is needed in order to compute the ATSP Measurements.

    //*********************************************************************************
    //**************************Classes************************************************
    //*********************************************************************************
    // DetectorConfig \belongs to\ DetectorByApproach \belongs to\ IntersectionByApproach
    public static class DetectorConfig{
        // This is the config for individual detectors
        public DetectorConfig(int _DetectorID,int _NumOfLanes,double _DistanceFromStopbar,String _Type,
                              boolean _IsExclusiveLeft,boolean _IsThroughInvolved,boolean _IsExclusiveRight,boolean _IsDuplicated){
            this.DetectorID=_DetectorID;
            this.NumOfLanes=_NumOfLanes;
            this.DistanceFromStopbar=_DistanceFromStopbar;
            this.Type=_Type;
            this.IsExclusiveLeft=_IsExclusiveLeft;
            this.IsThroughInvolved=_IsThroughInvolved;
            this.IsExclusiveRight=_IsExclusiveRight;
            this.IsDuplicated=_IsDuplicated;
        }
        public int DetectorID; // Detector ID
        public int NumOfLanes; // Number of lanes it covers
        // How far it is from the stopbar. Positive value: upstream of the stopbar; Negative value: downstream of the stopbar
        public double DistanceFromStopbar;
        public String Type; // Type of detectors: Advance or vehicle detector(stopbar detector??)
        public boolean IsExclusiveLeft; // Is it an exclusive left-turn detector?
        public boolean IsThroughInvolved; // Does it detector the through movement?
        public boolean IsExclusiveRight; // Is it an exclusive right-turn detector?
        // There may be multiple detectors on the same lanes to detect the traffic flow
        // Therefore, we may not use all of them for the calculation. We only choose a reliable one, and set others to be duplicated & unused.
        public boolean IsDuplicated;
    }

    public static class DetectorByApproach{
        // This is the detector config by approach
        public DetectorByApproach(String _StreetName, String _ApproachDirection, int _NumOfUpstreamLanes,int _NumOfDownstreamLanes,
                                  double _SpeedLimit,int _PhaseForLeftTurn,int _PhaseForThroughMovement,int _PhaseForRightTurn,
                                  int _LeadingPhaseID,List<DetectorConfig> _detectorConfigList){
            this.StreetName=_StreetName;
            this.ApproachDirection=_ApproachDirection;
            this.NumOfUpstreamLanes=_NumOfUpstreamLanes;
            this.NumOfDownstreamLanes=_NumOfDownstreamLanes;
            this.SpeedLimit=_SpeedLimit;
            this.PhaseForLeftTurn=_PhaseForLeftTurn;
            this.PhaseForThroughMovement=_PhaseForThroughMovement;
            this.PhaseForRightTurn=_PhaseForRightTurn;
            this.LeadingPhaseID=_LeadingPhaseID;
            this.detectorConfigList=_detectorConfigList;
        }
        public String StreetName; // Street name
        public String ApproachDirection; // EB, WB, SB, NB
        public int NumOfUpstreamLanes; // Number of upstream lanes
        public int NumOfDownstreamLanes; // Number of downstream lanes
        public double SpeedLimit; // Speed limit: used to project the arrival time from advance detector to the stopbar
        public int PhaseForLeftTurn; // What is the phase for left turn? If "Negative": not such a turn
        public int PhaseForThroughMovement; // What is the phase for straight through? If "Negative": not such a turn
        public int PhaseForRightTurn; // What is the phase for right turn? If "Negative": not such a turn
        public int LeadingPhaseID; // What is the leading(first) phase in a cycle, which is used for the purdue coordination diagram
        public List<DetectorConfig> detectorConfigList; // A list of detector configurations
    }

    public static class IntersectionProperty{
        // This is the profile for intersection property
        public IntersectionProperty(String _IntName,String _IntIPStr, List<DetectorByApproach> _detectorByApproachList,int [] _PedestrianPhases){
            this.IntName=_IntName;
            this.IntIPStr=_IntIPStr;
            this.detectorByApproachList=_detectorByApproachList;
            this.PedestrianPhases=_PedestrianPhases;
        }
        public String IntName;// Name of the intersection
        public String IntIPStr; // What is the IP address for this intersection
        public List<DetectorByApproach> detectorByApproachList; // A list of Detector Properties
        public int [] PedestrianPhases; // What are the pedestrian phases at this intersection.
    }


    //*********************************************************************************
    //**************************Hard-coded intersection properties*********************
    //*********************************************************************************
    public static List<IntersectionProperty> GetIntersectionProperties(){
        // This is the function to get intersection properties.
        // Currently, it is hard coded.

        List<IntersectionProperty> intersectionPropertyList=new ArrayList<IntersectionProperty>();

        // Temporary variables
        IntersectionProperty intersectionProperty;
        List<DetectorByApproach> detectorByApproachList;
        DetectorByApproach detectorByApproach;
        List<DetectorConfig> detectorConfigList;
        DetectorConfig detectorConfig;
        double SpeedLimit;
        int PhaseForLeftTurn;
        int PhaseForThroughMovement;
        int PhaseForRightTurn;
        int LeadingPhaseID;
        int [] PedestrianPhases;

        // **********************************Below are the intersections in our study site *******************************
        // **********************************Currently they are hard coded ***********************************************
        // **********************************
        // *****2nd Ave and Broad St*********
        // **********************************
        detectorByApproachList=new ArrayList<DetectorByApproach>();
        //Southbound
        // Data from Detector 1 looks bad, thus only Detector 2 is used (IsDuplicated=false).
        // Detector 3 seems to be in the upstream, thus we say it is not exclusive.
        detectorConfigList=new ArrayList<DetectorConfig>();
        detectorConfig= new DetectorConfig(1,1,0,"VehicleDetector",
                true,false,false,true);
        detectorConfigList.add(detectorConfig);
        detectorConfig= new DetectorConfig(2,1,0,"VehicleDetector",
                true,false,false,false);
        detectorConfigList.add(detectorConfig);

        detectorConfig= new DetectorConfig(3,1,50,"VehicleDetector",
                false,false,false,false);
        detectorConfigList.add(detectorConfig);

        SpeedLimit=25; // mph, can be obtained from the field
        // Phase settings in the field
        PhaseForLeftTurn=5;
        PhaseForRightTurn=2;
        PhaseForThroughMovement=2;
        LeadingPhaseID=2;
        detectorByApproach=new DetectorByApproach("2nd Ave", "SB",3,3,
                SpeedLimit,PhaseForLeftTurn,PhaseForThroughMovement,PhaseForRightTurn,LeadingPhaseID,detectorConfigList);
        detectorByApproachList.add(detectorByApproach);

        // Eastbound: detectors are installed at the exit lanes
        detectorConfigList=new ArrayList<DetectorConfig>();
        detectorConfig= new DetectorConfig(13,1,-50,"AdvanceDetector",
                false,true,false,false);
        detectorConfigList.add(detectorConfig);
        detectorConfig= new DetectorConfig(14,1,-50,"AdvanceDetector",
                false,true,false,false);
        detectorConfigList.add(detectorConfig);

        SpeedLimit=25; // mph
        PhaseForLeftTurn=-1; // No such a turn
        PhaseForRightTurn=4;
        PhaseForThroughMovement=4;
        LeadingPhaseID=2;
        detectorByApproach=new DetectorByApproach("Broad St", "EB",2,2,
                SpeedLimit,PhaseForLeftTurn,PhaseForThroughMovement,PhaseForRightTurn,LeadingPhaseID,detectorConfigList);
        detectorByApproachList.add(detectorByApproach);

        // Westbound
        detectorConfigList=new ArrayList<DetectorConfig>();
        detectorConfig= new DetectorConfig(29,1,50,"AdvanceDetector",
                false, true,false,false);
        detectorConfigList.add(detectorConfig);
        detectorConfig= new DetectorConfig(30,1,50,"AdvanceDetector",
                false,true,false,false);
        detectorConfigList.add(detectorConfig);
        detectorConfig= new DetectorConfig(31,1,50,"AdvanceDetector",
                false,true,false,false);
        detectorConfigList.add(detectorConfig);

        SpeedLimit=25; // mph
        PhaseForLeftTurn=4;
        PhaseForRightTurn=-1; // No right turn
        PhaseForThroughMovement=4;
        LeadingPhaseID=2;
        detectorByApproach=new DetectorByApproach("Broad St", "WB",3,2,
                SpeedLimit,PhaseForLeftTurn,PhaseForThroughMovement,PhaseForRightTurn,LeadingPhaseID,detectorConfigList);
        detectorByApproachList.add(detectorByApproach);

        // Pedestrian phases
        PedestrianPhases= new int[]{2,4}; // Phase 6 is not considered here

        intersectionProperty=new IntersectionProperty("2nd Ave@Broad St","172_16_48_145",detectorByApproachList,PedestrianPhases);
        intersectionPropertyList.add(intersectionProperty);

        // **********************************
        // *****2nd Ave and Battery St*********
        // **********************************
        detectorByApproachList=new ArrayList<DetectorByApproach>();
        //Southbound
        detectorConfigList=new ArrayList<DetectorConfig>();
        detectorConfig= new DetectorConfig(1,1,0,"VehicleDetector",
                true,false,false,true); // Detector 1 is not used since IsDuplicated=true
        detectorConfigList.add(detectorConfig);
        detectorConfig= new DetectorConfig(2,1,0,"VehicleDetector",
                true,false,false,false); // Use Detector 2
        detectorConfigList.add(detectorConfig);

        detectorConfig= new DetectorConfig(3,1,50,"VehicleDetector",
                false,false,false,false); // Detector 3 seems to be further upstream
        detectorConfigList.add(detectorConfig);

        SpeedLimit=25; // mph
        PhaseForLeftTurn=5;
        PhaseForRightTurn=-1;
        PhaseForThroughMovement=2;
        LeadingPhaseID=2;
        detectorByApproach=new DetectorByApproach("2nd Ave", "SB",3,3,
                SpeedLimit,PhaseForLeftTurn,PhaseForThroughMovement,PhaseForRightTurn,LeadingPhaseID,detectorConfigList);
        detectorByApproachList.add(detectorByApproach);

        //Eastbound: Detectors are installed at the exit lanes
        detectorConfigList=new ArrayList<DetectorConfig>();
        detectorConfig= new DetectorConfig(29,1,-50,"AdvanceDetector",
                false,true,false,false);
        detectorConfigList.add(detectorConfig);
        detectorConfig= new DetectorConfig(30,1,-50,"AdvanceDetector",
                false,true,false,false);
        detectorConfigList.add(detectorConfig);

        SpeedLimit=25; // mph
        PhaseForLeftTurn=-1;
        PhaseForRightTurn=4;
        PhaseForThroughMovement=4;
        LeadingPhaseID=2;
        detectorByApproach=new DetectorByApproach("Battery St", "EB",2,2,
                SpeedLimit,PhaseForLeftTurn,PhaseForThroughMovement,PhaseForRightTurn,LeadingPhaseID,detectorConfigList);
        detectorByApproachList.add(detectorByApproach);

        // Pedestrian phases
        PedestrianPhases= new int[]{2,4};// Phase 6 is not used here

        intersectionProperty=new IntersectionProperty("2nd Ave@Battery St","172_16_48_140",detectorByApproachList,PedestrianPhases);
        intersectionPropertyList.add(intersectionProperty);

        // **********************************
        // *****2nd Ave and Pike St*********
        // **********************************
        detectorByApproachList=new ArrayList<DetectorByApproach>();
        //Southbound: advance detectors are installed at exit lanes
        detectorConfigList=new ArrayList<DetectorConfig>();
        detectorConfig= new DetectorConfig(1,1,0,"VehicleDetector",
                true,false,false,false);
        detectorConfigList.add(detectorConfig);
        detectorConfig= new DetectorConfig(2,1,0,"VehicleDetector",
                true,false,false,true); // Detector 2 is not used here since IsDuplicated=true
        detectorConfigList.add(detectorConfig);

        detectorConfig= new DetectorConfig(3,1,50,"VehicleDetector",
                false,false,false,false); // Detector 3 seems to be further upstream
        detectorConfigList.add(detectorConfig);

        detectorConfig= new DetectorConfig(13,1,-50,"AdvanceDetector",
                false,true,false,false);
        detectorConfigList.add(detectorConfig);
        detectorConfig= new DetectorConfig(14,1,-50,"AdvanceDetector",
                false,true,false,false);
        detectorConfigList.add(detectorConfig);
        detectorConfig= new DetectorConfig(15,1,-50,"AdvanceDetector",
                false,true,false,false);
        detectorConfigList.add(detectorConfig);

        SpeedLimit=25; // mph
        PhaseForLeftTurn=5;
        PhaseForRightTurn=-1;
        PhaseForThroughMovement=2;
        LeadingPhaseID=2;
        detectorByApproach=new DetectorByApproach("2nd Ave", "SB",4,4,
                SpeedLimit,PhaseForLeftTurn,PhaseForThroughMovement,PhaseForRightTurn,LeadingPhaseID,detectorConfigList);
        detectorByApproachList.add(detectorByApproach);

        // Pedestrian phases
        PedestrianPhases= new int[]{2,4};// Phase 6 is not considered

        intersectionProperty=new IntersectionProperty("2nd Ave@Pike St","172_16_48_158",detectorByApproachList,PedestrianPhases);
        intersectionPropertyList.add(intersectionProperty);

        // **********************************
        // *****2nd Ave and Spring St*********
        // **********************************
        detectorByApproachList=new ArrayList<DetectorByApproach>();
        //Southbound: advance detectors are installed at exit lanes
        detectorConfigList=new ArrayList<DetectorConfig>();
        detectorConfig= new DetectorConfig(1,1,0,"VehicleDetector",
                true,false,false,false);
        detectorConfigList.add(detectorConfig);
        detectorConfig= new DetectorConfig(2,1,0,"VehicleDetector",
                true,false,false,true); // Detector 2 is not used sinde IsDuplicated=true
        detectorConfigList.add(detectorConfig);

        detectorConfig= new DetectorConfig(3,1,50,"VehicleDetector",
                false,false,false,false); // Detector 3 seems to be further upstream
        detectorConfigList.add(detectorConfig);

        detectorConfig= new DetectorConfig(29,1,-50,"AdvanceDetector",
                false,true,false,false);
        detectorConfigList.add(detectorConfig);
        detectorConfig= new DetectorConfig(30,1,-50,"AdvanceDetector",
                false,true,false,false);
        detectorConfigList.add(detectorConfig);
        detectorConfig= new DetectorConfig(31,1,-50,"AdvanceDetector",
                false,true,false,false);
        detectorConfigList.add(detectorConfig);

        SpeedLimit=25; // mph
        PhaseForLeftTurn=5;
        PhaseForRightTurn=-1;
        PhaseForThroughMovement=2;
        LeadingPhaseID=2;
        detectorByApproach=new DetectorByApproach("2nd Ave", "SB",4,4,
                SpeedLimit,PhaseForLeftTurn,PhaseForThroughMovement,PhaseForRightTurn,LeadingPhaseID,detectorConfigList);
        detectorByApproachList.add(detectorByApproach);

        // Pedestrian phases
        PedestrianPhases= new int[]{2,4};

        intersectionProperty=new IntersectionProperty("2nd Ave@Spring St","172_16_48_164",detectorByApproachList,PedestrianPhases);
        intersectionPropertyList.add(intersectionProperty);

        // Return values
        return intersectionPropertyList;
    }


    //*********************************************************************************
    //**************************Functions**********************************************
    //*********************************************************************************
    public static List<DetectorByApproach> getDetectorByApproachWithGivenIntIPStr(String IntIPStr,List<IntersectionProperty> intersectionPropertyList){
        // This function is used to get the list of detectors by approach with given the intersection IP address
        // Intersection IP is used as a unique ID for a given intersection here
        List<DetectorByApproach> detectorByApproachList=new ArrayList<DetectorByApproach>();

        for(int i=0;i<intersectionPropertyList.size();i++){
            if(intersectionPropertyList.get(i).IntIPStr.equals(IntIPStr)){
                detectorByApproachList=intersectionPropertyList.get(i).detectorByApproachList;
                break;
            }
        }
        return detectorByApproachList;
    }

    public static String getIntNameWithGivenIntIPStr(String IntIPStr,List<IntersectionProperty> intersectionPropertyList){
        // This is the function to get an intersection's name with a given IP
        String IntName=null;
        for(int i=0;i<intersectionPropertyList.size();i++){
            if(intersectionPropertyList.get(i).IntIPStr.equals(IntIPStr)){
                IntName=intersectionPropertyList.get(i).IntName;
                break;
            }
        }
        return IntName;
    }

    public static List<DetectorConfig> getListOfDetectorsByGivenType(List<DetectorConfig> detectorConfigList,String Type,String TurnMovement){
        // This function is to select a set of detectors that satisfy the requirement of "Type" and "Turn Movement"

        List<DetectorConfig> detectorConfigList1 =new ArrayList<DetectorConfig>();
        for(int i=0;i<detectorConfigList.size();i++){
            if(Type.equals("AdvanceDetector")) { // Advance detectors
                if (detectorConfigList.get(i).Type.equals(Type)) {
                    detectorConfigList1.add(detectorConfigList.get(i));
                }
            }else if (Type.equals("VehicleDetector")){ // Vehicle detectors
                if(detectorConfigList.get(i).Type.equals(Type)){
                    if(TurnMovement.equals("LeftTurn")){// If it is exclusive left turn at the stopbar
                        if(detectorConfigList.get(i).IsExclusiveLeft && !detectorConfigList.get(i).IsDuplicated && detectorConfigList.get(i).DistanceFromStopbar==0) {
                            detectorConfigList1.add(detectorConfigList.get(i));
                        }
                    }else if(TurnMovement.equals("RightTurn")){// If it is exclusive right turn at the stopbar
                        if(detectorConfigList.get(i).IsExclusiveRight && !detectorConfigList.get(i).IsDuplicated && detectorConfigList.get(i).DistanceFromStopbar==0) {
                            detectorConfigList1.add(detectorConfigList.get(i));
                        }
                    }else{
                        System.out.println("Please choose left-/right-turns!");
                        System.exit(-1);
                    }
                }
            }else{
                System.out.println("Unknown detector type!");
                System.exit(-1);
            }
        }
        return detectorConfigList1;
    }

    public static int[] getPedestrianPhasesWithGivenIntIPStr(String IntIPStr, List<IntersectionProperty>intersectionPropertyList){
        // This function is used to get pedestrian's phases with a given intersection IP
        int [] pedestrianPhases=null;
        for(int i=0;i<intersectionPropertyList.size();i++){
            if(intersectionPropertyList.get(i).IntIPStr.equals(IntIPStr)){
                pedestrianPhases=intersectionPropertyList.get(i).PedestrianPhases;
                break;
            }
        }
        return pedestrianPhases;
    }
}
