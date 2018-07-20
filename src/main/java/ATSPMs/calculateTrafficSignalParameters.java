package ATSPMs;

import ATSPMs.IntersectionConfig.*;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.StandardChartTheme;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.NumberTickUnit;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.*;
import org.jfree.chart.title.LegendTitle;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.ui.ApplicationFrame;
import org.jfree.ui.RectangleEdge;
import org.jfree.util.ShapeUtilities;
import org.jfree.chart.renderer.xy.XYSplineRenderer;
import java.awt.Color;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import java.awt.geom.Ellipse2D;

import java.awt.*;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;
import java.util.List;

public class calculateTrafficSignalParameters {
// This function is used to calculate the traffic signal parameters

    //******************************************************************************
    //******************************Event Properties********************************
    //******************************************************************************
    public static class EventProperties{
        // This is the profile for Event ID, Time, and Event Value
        public EventProperties(List<Double> _eventTimes, List<Integer> _eventIDs, List<Integer> _eventValues){
            this.eventIDs=_eventIDs;
            this.eventTimes=_eventTimes;
            this.eventValues=_eventValues;
        }
        public List<Double> eventTimes;
        public List<Integer> eventIDs;
        public List<Integer> eventValues;
    }

    public static EventProperties getEventPropertiesFromResultSet(ResultSet resultSet){
        // This function is to get Event ID, Time and Value From ResultSet
        List<Double> eventTimes=new ArrayList<Double>();
        List<Integer> eventIDs=new ArrayList<Integer>();
        List<Integer> eventValues=new ArrayList<Integer>();

        try{
            while(resultSet.next()){
                double eventTime=resultSet.getDouble("Time");
                int eventID=resultSet.getInt("EventID");
                int eventValue=resultSet.getInt("EventValue");
                eventTimes.add(eventTime);
                eventIDs.add(eventID);
                eventValues.add(eventValue);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        EventProperties eventProperties=new EventProperties(eventTimes,eventIDs,eventValues);
        return eventProperties;
    }


    //************************************************************************************
    //******************************Signal Plan ******************************************
    //************************************************************************************
    public static class PlanProperty{
        // This is the profile for phase property
        public PlanProperty(int _PlanID, double _StartTime, double _EndTime,double _CycleLength,
                            double _Offset, double[] _GreenSplits){
            this.PlanID=_PlanID;
            this.StartTime=_StartTime;
            this.EndTime=_EndTime;
            this.CycleLength=_CycleLength;
            this.Offset=_Offset;
            this.GreenSplits=_GreenSplits;
        }
        public int PlanID;
        public double StartTime;
        public double EndTime;
        public double CycleLength;
        public double Offset;
        public double [] GreenSplits;// Green splits for the phases
    }

    public static List<PlanProperty> getPhasePlanSequenceForGivenDateAndTimePeriod(int Date, double FromTime, double ToTime,
                                                                     String IntIPStr, Connection con){
        // This function is used to get the plan sequence for a given phase on a given date at given time periods
        List<PlanProperty> planPropertyList=null;

        int Year=Date/10000;
        String DataTable=IntIPStr+"_"+Year;
        try{
            Statement ps=con.createStatement();
            String sql="Select Time,EventID,EventValue From "+ DataTable + " Where Date="+Date + " And Time>="+FromTime
                    + " And Time<="+ToTime + " And EventID>=131 And EventID<=149;";
            ResultSet resultSet=ps.executeQuery(sql);
            EventProperties eventProperties=getEventPropertiesFromResultSet(resultSet);
            planPropertyList=getPlanProperties(eventProperties,FromTime,ToTime);
        } catch (SQLException e) {
            e.printStackTrace();
        }

        // Get unique plan properties
        List<PlanProperty> newPlanPropertyList=findUniquePlanSequence(planPropertyList);
        return newPlanPropertyList;
    }

    public static List<PlanProperty> findUniquePlanSequence(List<PlanProperty> planPropertyList){
        // This function is used to find unique plan sequence & get rid of phase properties with incomplete information

        List<PlanProperty> newPlanPropertyList=new ArrayList<PlanProperty>();
        if(planPropertyList.size()==0){
            System.out.println("Empty list of plan properties!");
            System.exit(-1);
        }else{
            int Index=-1;
            // Get the first valid plan property
            for(int i=0;i<planPropertyList.size();i++){
                if(planPropertyList.get(i).CycleLength>=0 && planPropertyList.get(i).Offset>=0){
                    newPlanPropertyList.add(planPropertyList.get(i));
                    Index=i;
                    break;
                }
            }
            if(Index==-1){
                System.out.println("Useless list of plan properties!");
                System.exit(-1);
            }else{
                // We do see sometimes only the plan ID is reported, but no other information!
                for(int i=Index+1;i<planPropertyList.size();i++){
                    // Check whether the splits are empty
                    double SplitSum=0;
                    for(int j=0;j<planPropertyList.get(i).GreenSplits.length;j++){
                        SplitSum=SplitSum+planPropertyList.get(i).GreenSplits[j];
                    }

                    if(planPropertyList.get(i).CycleLength!=-1&& planPropertyList.get(i).Offset!=-1 && SplitSum>0 ){// Not empty!
                        newPlanPropertyList.add(planPropertyList.get(i)); // Add it to the end
                    }else{
                        // With incomplete information
                        if(newPlanPropertyList.get(newPlanPropertyList.size()-1).PlanID==planPropertyList.get(i).PlanID){// The same Plan ID
                            if(planPropertyList.get(i).CycleLength==-1&& planPropertyList.get(i).Offset==-1 && SplitSum>0){
                                // Different in Green Splits?
                                // Update with the same cycle length and offset
                                planPropertyList.get(i).CycleLength=newPlanPropertyList.get(newPlanPropertyList.size()-1).CycleLength;
                                planPropertyList.get(i).Offset=newPlanPropertyList.get(newPlanPropertyList.size()-1).Offset;
                                // Add it to the end
                                newPlanPropertyList.add(planPropertyList.get(i));
                            }else if(planPropertyList.get(i).CycleLength!=-1&& planPropertyList.get(i).Offset!=-1 && SplitSum==0){
                                // Different in Cycle length and Offset?
                                // Update with the same green splits
                                planPropertyList.get(i).GreenSplits=newPlanPropertyList.get(newPlanPropertyList.size()-1).GreenSplits;
                                // Add it to the end
                                newPlanPropertyList.add(planPropertyList.get(i));
                            }else{
                                // If none of the above, just extend the EndTime
                                newPlanPropertyList.get(newPlanPropertyList.size()-1).EndTime=planPropertyList.get(i).EndTime;// Change the end time
                            }
                        }else { // If not the same plan ID, just extend the EndTime
                            newPlanPropertyList.get(newPlanPropertyList.size() - 1).EndTime = planPropertyList.get(i).EndTime;// Change the end time
                        }
                    }
                }
            }
        }
        return newPlanPropertyList;
    }

    public static List<PlanProperty> getPlanProperties(EventProperties eventProperties, double FromTime,
                                                        double ToTime){
        // This function is used to get phase properties
        List<PlanProperty> planPropertyList=new ArrayList<PlanProperty>();

        List<Double> eventTimes=eventProperties.eventTimes;
        List<Integer> eventIDs=eventProperties.eventIDs;
        List<Integer> eventValues=eventProperties.eventValues;
        for(int i=0;i<eventTimes.size();i++){
            if(eventIDs.get(i)==131){
                int PlanID=eventValues.get(i);
                double StartTime=eventTimes.get(i);
                if(planPropertyList.size()>0){
                    planPropertyList.get(planPropertyList.size()-1).EndTime=Math.min(StartTime,ToTime);
                }
                double CycleLength=-1;
                double Offset=-1;
                double [] GreenSplits=new double[]{0,0,0,0, 0,0,0,0, 0,0,0,0, 0,0,0,0};

                int j=i+1;
                while(j<eventTimes.size()){
                    if(eventIDs.get(j)==131){// Find a new plan
                        j=j-1;
                        break;
                    }else if(eventIDs.get(j)==132) // Cycle length
                        CycleLength=eventValues.get(j);
                    else if(eventIDs.get(j)==133) // Offset
                        Offset=eventValues.get(j);
                    else if(eventIDs.get(j)>=134&&eventIDs.get(j)<=149){ //Green splits
                        int idx=eventIDs.get(j)-134;
                        GreenSplits[idx]=eventValues.get(j);
                    }else{
                        System.out.println("Unrecognized event ID!");
                        System.exit(-1);
                    }
                    j=j+1;
                }
                i=j;
                PlanProperty planProperty=new PlanProperty(PlanID,StartTime,ToTime,CycleLength,Offset,GreenSplits);
                planPropertyList.add(planProperty);
            }
        }
        return planPropertyList;
    }

    public static XYSeries getXTimeYMeasurePlannedRev(List<PlanProperty> planPropertyList,int PlanID, int PhaseID){
        // This function is used to get (planned) measurements (Time & Duration/Green Time) for given plan ID and Phase ID

        // It returns a array list with MaxPoint+1 (for drawing either by line or points)
        List<double[]> TimeAndDuration=new ArrayList<double[]>();
        for(int p=0;p<planPropertyList.size();p++) {
            if(planPropertyList.get(p).PlanID==PlanID) {
                PlanProperty planProperty=planPropertyList.get(p);
                int MaxPoint = 100;
                double planGreen = planProperty.GreenSplits[PhaseID - 1]; // Green time for the given phase ID
                double StartTime = planProperty.StartTime;
                double EndTime = planProperty.EndTime;
                double DeltaTime = (EndTime - StartTime) / MaxPoint;
                TimeAndDuration.add(new double[]{StartTime / 3600.0, planGreen});
                for (int i = 0; i < MaxPoint; i++) {
                    TimeAndDuration.add(new double[]{(StartTime + (i + 1) * DeltaTime) / 3600.0, planGreen});
                }
            }
        }
        XYSeries xySeries=new XYSeries(TimeAndDuration.size()); // Get a new xy series
        for(int i=0;i<TimeAndDuration.size();i++){
            xySeries.add(TimeAndDuration.get(i)[0],TimeAndDuration.get(i)[1]);
        }
        return xySeries;
    }

    public static XYSeries getXTimeYMeasureByTimingStatusRev(double[] TimeInSecond, double [] YMeasure, List<PlanProperty>
            planPropertyList, int PlanID){
        // This function is used to get (actual) x-y measures (Time & Duration/Green Time) with given a given plan ID

        List<Double> selectedTime=new ArrayList<Double>();
        List<Double> selectedYMeasure=new ArrayList<Double>();
        for(int p=0;p<planPropertyList.size();p++) {
            if(planPropertyList.get(p).PlanID==PlanID) {
                PlanProperty planProperty = planPropertyList.get(p);
                double StartTime = planProperty.StartTime;
                double EndTime = planProperty.EndTime;
                for (int i = 0; i < TimeInSecond.length; i++) { // Loop for each row
                    if (TimeInSecond[i] >= StartTime && TimeInSecond[i] <= EndTime) {
                        // Add a new point
                        selectedTime.add(TimeInSecond[i] / 3600.0);
                        selectedYMeasure.add(YMeasure[i]);
                    }
                }
            }
        }

        if(selectedTime.size()>0){
            XYSeries xySeries=new XYSeries(selectedTime.size()); // Get a new xy series
            for(int i=0;i<selectedTime.size();i++){
                xySeries.add(selectedTime.get(i), selectedYMeasure.get(i));
            }
            return xySeries;
        }else{
            return null;
        }
    }

    //************************************************************************************
    //******************************Signal Phase******************************************
    //************************************************************************************
    public static class PhaseProperty{
        // This is the profile for actual phase property
        public PhaseProperty(double _GreenStartTime,double _GreenEndTime,double _GreenDuration,double _MinGreenDuration,
                             double _YellowStartTime,double _YellowEndTime,double _YellowDuration,
                             double _AllRedStartTime,double _AllRedEndTime,double _AllRedDuration,
                             boolean _IsMinGreenComplete, boolean _IsGapOut, boolean _IsMaxOut, boolean _IsForceOff){
            this.GreenStartTime=_GreenStartTime;
            this.GreenEndTime=_GreenEndTime;
            this.GreenDuration=_GreenDuration;
            this.MinGreenDuration=_MinGreenDuration;
            this.IsMinGreenComplete=_IsMinGreenComplete;

            this.YellowStartTime=_YellowStartTime;
            this.YellowEndTime=_YellowEndTime;
            this.YellowDuration=_YellowDuration;

            this.AllRedStartTime=_AllRedStartTime;
            this.AllRedEndTime=_AllRedEndTime;
            this.AllRedDuration=_AllRedDuration;

            this.IsGapOut=_IsGapOut;
            this.IsMaxOut=_IsMaxOut;
            this.IsForceOff=_IsForceOff;
        }
        public double GreenStartTime;
        public double GreenEndTime;
        public double GreenDuration;
        public double MinGreenDuration;
        public boolean IsMinGreenComplete=false;

        public double YellowStartTime;
        public double YellowEndTime;
        public double YellowDuration;

        public double AllRedStartTime;
        public double AllRedEndTime;
        public double AllRedDuration;

        public boolean IsGapOut=false;
        public boolean IsMaxOut=false;
        public boolean IsForceOff=false;
    }

    public static class PhasePropertyByID{
        public PhasePropertyByID(int _PhaseID, List<PhaseProperty> _phasePropertyList){
            this.PhaseID=_PhaseID;
            this.phasePropertyList=_phasePropertyList;
        }
        public int PhaseID;
        public List<PhaseProperty> phasePropertyList;
    }

    public static class YellowTimeProperty{
        // This is the profile for yellow time property
        public YellowTimeProperty(double _YellowStartTime,double _YellowEndTime,double _YellowDuration){
            this.YellowStartTime=_YellowStartTime;
            this.YellowEndTime=_YellowEndTime;
            this.YellowDuration=_YellowDuration;
        }
        public double YellowStartTime;
        public double YellowEndTime;
        public double YellowDuration;
    }

    public static class RedTimeProperty{
        // This is the profile for red time property
        public RedTimeProperty(double _AllRedStartTime,double _AllRedEndTime,double _AllRedDuration){
            this.AllRedStartTime=_AllRedStartTime;
            this.AllRedEndTime=_AllRedEndTime;
            this.AllRedDuration=_AllRedDuration;
        }
        public double AllRedStartTime;
        public double AllRedEndTime;
        public double AllRedDuration;
    }

    public static class YellowAndAllRedPropertyByID{
        // This is the profile for yellow and all red times
        public YellowAndAllRedPropertyByID(int _PhaseID,List<YellowTimeProperty> _yellowTimePropertyList,
                                           List<RedTimeProperty> _redTimePropertyList){
            this.PhaseID=_PhaseID;
            this.yellowTimePropertyList=_yellowTimePropertyList;
            this.redTimePropertyList=_redTimePropertyList;
        }
        public int PhaseID;
        public List<YellowTimeProperty> yellowTimePropertyList;
        public List<RedTimeProperty> redTimePropertyList;
    }

    public static List<PhasePropertyByID> getActualPhaseInfForGivenDateAndTimePeriod(int Date, double FromTime, double ToTime,
                                                                  String IntIPStr, Connection con){
        //This function is used to get actual phase information for given date and time period
        List<PhasePropertyByID> actualPhasePropertyList=new ArrayList<PhasePropertyByID>();

        int Year=Date/10000;
        String DataTable=IntIPStr+"_"+Year;
        try{
            Statement ps=con.createStatement();
            String sql="Select Time,EventID,EventValue From "+ DataTable + " Where Date="+Date + " And Time>="+FromTime
                    + " And Time<="+ToTime + " And EventID>=0 And EventID<=12;";
            ResultSet resultSet=ps.executeQuery(sql);
            EventProperties eventProperties=getEventPropertiesFromResultSet(resultSet);
            List<Integer> phaseIDList=getListOfPhaseIDsFromEventProperties(eventProperties);
            for(int i=0;i<phaseIDList.size();i++){
                PhasePropertyByID phasePropertyByID=getActualPhaseProperties(eventProperties, phaseIDList.get(i));
                actualPhasePropertyList.add(phasePropertyByID);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return actualPhasePropertyList;
    }

    public static PhasePropertyByID getActualPhaseInfForGivenPhaseDateAndTimePeriod(int Date, double FromTime
            , double ToTime,int PhaseID,String IntIPStr, Connection con){
        //This function is used to get actual phase information for given Phase ID, date and time period

        PhasePropertyByID phasePropertyByID=new PhasePropertyByID(PhaseID,null);
         int Year=Date/10000;
        String DataTable=IntIPStr+"_"+Year;
        try{
            Statement ps=con.createStatement();
            String sql="Select Time,EventID,EventValue From "+ DataTable + " Where Date="+Date + " And Time>="+FromTime
                    + " And Time<="+ToTime + " And EventID>=0 And EventID<=12 And EventValue="+PhaseID+" order by Time;";
            ResultSet resultSet=ps.executeQuery(sql);
            EventProperties eventProperties=getEventPropertiesFromResultSet(resultSet);
            phasePropertyByID=getActualPhaseProperties(eventProperties, PhaseID);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return phasePropertyByID;
    }

    public static YellowAndAllRedPropertyByID getActualYellowAndRedInfForGivenPhaseDateAndTimePeriod(int Date, double FromTime
            , double ToTime,int PhaseID,String IntIPStr, Connection con){
        //This function is used to get actual yellow and red information for given Phase ID, date and time period

        YellowAndAllRedPropertyByID yellowAndAllRedPropertyByID=new YellowAndAllRedPropertyByID(PhaseID,null,null);
        int Year=Date/10000;
        String DataTable=IntIPStr+"_"+Year;
        try{
            Statement ps=con.createStatement();
            String sql="Select Time,EventID,EventValue From "+ DataTable + " Where Date="+Date + " And Time>="+FromTime
                    + " And Time<="+ToTime + " And EventID>=0 And EventID<=12 And EventValue="+PhaseID+" order by Time;";
            ResultSet resultSet=ps.executeQuery(sql);
            EventProperties eventProperties=getEventPropertiesFromResultSet(resultSet);
            yellowAndAllRedPropertyByID=getYellowAndAllRedDurations(eventProperties, PhaseID);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return yellowAndAllRedPropertyByID;
    }

    public static List<double[]> adjustVehicleOffsetsAccordingToYellowTimes(List<ActuationEventByDetector> actuationEventByDetectorList
            , List<YellowTimeProperty> yellowTimePropertyList){
        // This is used to adjust the vehicle offsets according to the yellow times

        List<double[]> VehicleOffSet=new ArrayList<double[]>();

        // Get the yellow start times
        List<Double> YellowStartTimes=new ArrayList<Double>();
        for(int i=0;i<yellowTimePropertyList.size();i++){
            if(yellowTimePropertyList.get(i).YellowStartTime>0 && yellowTimePropertyList.get(i).YellowDuration>0) {
                YellowStartTimes.add(yellowTimePropertyList.get(i).YellowStartTime);
            }
        }
        Collections.sort(YellowStartTimes);

        // Get the vehicle actuation offsets
        double MaxDuration=20;
        for(int i=0;i<actuationEventByDetectorList.size();i++){// Loop for each detector ID
            List<ActuationEvent> actuationEventList=actuationEventByDetectorList.get(i).actuationEventList;
            for(int j=0;j<actuationEventList.size();j++){// Loop for each actuation event
                double OffTime=actuationEventList.get(j).OffTime;
                int Index=-1;
                for(int k=0;k<YellowStartTimes.size();k++){// Find the closest yellow start time
                    if(OffTime<YellowStartTimes.get(k)){
                        Index=k-1;
                        break;
                    }
                }
                if(Index>=0){// Find the right index?
                    double TimeOffset=OffTime-YellowStartTimes.get(Index);
                    if(TimeOffset<MaxDuration){ // Withing the maximum duration??
                        VehicleOffSet.add(new double[]{YellowStartTimes.get(Index),OffTime,TimeOffset});
                    }
                }
            }
        }
        return VehicleOffSet;
    }

    public static List<double[]> adjustAllRedOffsetsAccordingToYellowTimes(List<RedTimeProperty> redTimePropertyList,
             List<YellowTimeProperty>yellowTimePropertyList){
        // This is the function used to adjust the offsets for all red times according to the yellow times

        List<double[]> AllRedOffSets=new ArrayList<double[]>();

        // Get the yellow start times
        List<Double> YellowStartTimes=new ArrayList<Double>();
        for(int i=0;i<yellowTimePropertyList.size();i++){
            if(yellowTimePropertyList.get(i).YellowStartTime>0 && yellowTimePropertyList.get(i).YellowDuration>0) {
                YellowStartTimes.add(yellowTimePropertyList.get(i).YellowStartTime);
            }
        }
        Collections.sort(YellowStartTimes);

        // Get the all red offsets
        double MaxDuration=10;
        for(int j=0;j<redTimePropertyList.size();j++){// Loop for each all red event
            double EndTime=redTimePropertyList.get(j).AllRedEndTime;
            double StartTime=redTimePropertyList.get(j).AllRedStartTime;
            int Index=-1;
            for(int k=0;k<YellowStartTimes.size();k++){// Find the closest yellow start time
                if(EndTime<YellowStartTimes.get(k)){
                    Index=k-1;
                    break;
                }
            }
            if(Index>=0){// Find the right index?
                double TimeOffset=EndTime-YellowStartTimes.get(Index); //Offset for Red End Time
                double TimeOffset2=StartTime-YellowStartTimes.get(Index); // Offset for Red Start Time
                if(TimeOffset<MaxDuration){
                    AllRedOffSets.add(new double[]{YellowStartTimes.get(Index),EndTime,TimeOffset,TimeOffset2});
                }
            }
        }
        return AllRedOffSets;
    }

    public static List<Integer> getListOfPhaseIDsFromEventProperties(EventProperties eventProperties){
        // This function is used to get the list of phase IDs from event properties (actual phase events)
        List<Integer> PhaseIDList=new ArrayList<Integer>();
        HashSet<Integer> hashSet=new HashSet<Integer>();

        List<Double> eventTimes=eventProperties.eventTimes;
        List<Integer> eventIDs=eventProperties.eventIDs;
        List<Integer> eventValues=eventProperties.eventValues;
        for(int i=0;i<eventTimes.size();i++){
            if(eventIDs.get(i)==0){
                if(hashSet.add(eventValues.get(i))){
                    PhaseIDList.add(eventValues.get(i));
                }
            }
        }
        return PhaseIDList;
    }

    public static YellowAndAllRedPropertyByID getYellowAndAllRedDurations(EventProperties eventProperties, int phaseID){
        // This function is used to get yellow and all red times

        List<Double> eventTimes=eventProperties.eventTimes;
        List<Integer> eventIDs=eventProperties.eventIDs;
        List<Integer> eventValues=eventProperties.eventValues;

        double MaxYellowDuration=10;
        List<YellowTimeProperty> yellowTimePropertyList=new ArrayList<YellowTimeProperty>();
        for(int i=0;i<eventTimes.size();i++){
            if(eventIDs.get(i)==8 && eventValues.get(i)==phaseID){
                // Yellow begins
                double YellowStartTime=eventTimes.get(i);
                double YellowEndTime=-1;
                double YellowDuration=-1;

                int j=i+1;
                while(j<eventTimes.size()){// Search the next events
                    if(eventValues.get(j)==phaseID) {
                        if(eventIDs.get(j)==9){ // Find the end of yellow
                            YellowEndTime=eventTimes.get(j);
                            YellowDuration=YellowEndTime-YellowStartTime;
                            if(YellowDuration<MaxYellowDuration){// If smaller than the max duration
                                YellowTimeProperty yellowTimeProperty=new YellowTimeProperty(YellowStartTime,YellowEndTime,YellowDuration);
                                yellowTimePropertyList.add(yellowTimeProperty); // Append to the end
                                i=j; // Adjust the i value
                            }// If it is greater than the max duration, do not adjust the i value. Let the program start from the next i+1
                            break;
                        }
                    }
                    j=j+1;// Find the next event
                }
            }
        }

        List<RedTimeProperty> redTimePropertyList=new ArrayList<RedTimeProperty>();
        double MaxRedDuration=5;
        for(int i=0;i<eventTimes.size();i++){
            if(eventIDs.get(i)==10 && eventValues.get(i)==phaseID){
                // Red begins
                double RedStartTime=eventTimes.get(i);
                double RedEndTime=-1;
                double RedDuration=-1;

                int j=i+1;
                while(j<eventTimes.size()){// Search the next events
                    if(eventValues.get(j)==phaseID) {
                        if(eventIDs.get(j)==11){ // Find the end of red
                            RedEndTime=eventTimes.get(j);
                            RedDuration=RedEndTime-RedStartTime;
                            if(RedDuration<MaxRedDuration){// If smaller than the max duration
                                RedTimeProperty redTimeProperty=new RedTimeProperty(RedStartTime,RedEndTime,RedDuration);
                                redTimePropertyList.add(redTimeProperty); // Append to the end
                                i=j; // Adjust the i value
                            }// If it is greater than the max duration, do not adjust the i value. Let the program start from the next i+1
                            break;
                        }
                    }
                    j=j+1;// Find the next event
                }
            }
        }
        YellowAndAllRedPropertyByID yellowAndAllRed=new YellowAndAllRedPropertyByID(phaseID,yellowTimePropertyList,redTimePropertyList);
        return yellowAndAllRed;
    }

    public static PhasePropertyByID getActualPhaseProperties(EventProperties eventProperties, int phaseID){
        // This function is used to get actual phase properties

        List<PhaseProperty> actualPhasePropertyList=new ArrayList<PhaseProperty>();

        List<Double> eventTimes=eventProperties.eventTimes;
        List<Integer> eventIDs=eventProperties.eventIDs;
        List<Integer> eventValues=eventProperties.eventValues;

        for(int i=0;i<eventTimes.size();i++){
            if(eventIDs.get(i)==0 && eventValues.get(i)==phaseID){
                // Phase begin
                double GreenStartTime=eventTimes.get(i);
                double GreenEndTime=-1;
                double GreenDuration=-1;
                double MinGreenDuration=-1;
                boolean IsMinGreenComplete=false;

                double YellowStartTime=-1;
                double YellowEndTime=-1;
                double YellowDuration=-1;

                double AllRedStartTime=-1;
                double AllRedEndTime=-1;
                double AllRedDuration=-1;

                boolean IsGapOut=false;
                boolean IsMaxOut=false;
                boolean IsForceOff=false;
                double GapOutTime=-1;
                double MaxOutTime=-1;
                double ForceOffTime=-1;
                int j=i+1;
                while(j<eventTimes.size()){
                    if(eventValues.get(j)==phaseID) {
                        if (eventIDs.get(j) == 0 ) {// A new cycle??
                            if(GreenEndTime<0) {
                                // Adjust the green end time if it is negative
                                if (GapOutTime > 0) {
                                    GreenEndTime = GapOutTime;
                                    GreenDuration = GreenEndTime - GreenStartTime;
                                }
                                if (MaxOutTime > 0) {
                                    GreenEndTime = MaxOutTime;
                                    GreenDuration = GreenEndTime - GreenStartTime;
                                }
                                if (ForceOffTime > 0) {
                                    GreenEndTime = ForceOffTime;
                                    GreenDuration = GreenEndTime - GreenStartTime;
                                }
                            }
                            j = j - 1;
                            break;
                        }else if (eventIDs.get(j) == 3) {
                            MinGreenDuration = eventTimes.get(j) - GreenStartTime;
                            IsMinGreenComplete = true;
                        }else if(eventIDs.get(j)==4){
                            IsGapOut=true;
                            GapOutTime=eventTimes.get(j);
                        }else if(eventIDs.get(j)==5){
                            IsMaxOut=true;
                            MaxOutTime=eventTimes.get(j);
                        }else if(eventIDs.get(j)==6){
                            IsForceOff=true;
                            ForceOffTime=eventTimes.get(j);
                        }else if(eventIDs.get(j)==7){
                            GreenEndTime=eventTimes.get(j);
                            GreenDuration=GreenEndTime-GreenStartTime;
                        }else if(eventIDs.get(j)==8){
                            YellowStartTime=eventTimes.get(j);
                        }else if(eventIDs.get(j)==9){
                            YellowEndTime=eventTimes.get(j);
                            YellowDuration=YellowEndTime-YellowStartTime;
                        }else if(eventIDs.get(j)==10){
                            AllRedStartTime=eventTimes.get(j);
                        }else if(eventIDs.get(j)==11){
                            AllRedEndTime=eventTimes.get(j);
                            AllRedDuration=AllRedEndTime-AllRedStartTime;
                        }
                    }
                    j=j+1;
                }
                i=j;
                PhaseProperty phaseProperty = new PhaseProperty(GreenStartTime, GreenEndTime, GreenDuration, MinGreenDuration,
                        YellowStartTime, YellowEndTime, YellowDuration, AllRedStartTime, AllRedEndTime, AllRedDuration,
                        IsMinGreenComplete, IsGapOut, IsMaxOut, IsForceOff);
                actualPhasePropertyList.add(phaseProperty);
            }
        }
        PhasePropertyByID phasePropertyByID=new PhasePropertyByID(phaseID,actualPhasePropertyList);
        return phasePropertyByID;
    }

    public static List<Integer> findUniquePlanIDs(List<PlanProperty> planPropertyList){
        // This function is used to find Unique plan IDs
        List<Integer> UniquePlanIDs=new ArrayList<Integer>();

        HashSet<Integer> hashSet=new HashSet<Integer>();
        for(int i=0;i<planPropertyList.size();i++){
            if(hashSet.add(planPropertyList.get(i).PlanID)){
                UniquePlanIDs.add(planPropertyList.get(i).PlanID);
            }
        }
        return UniquePlanIDs;
    }

    //******************************************************************************
    //******************************Detector Events ********************************
    //******************************************************************************
    public static class ActuationEvent{
        // This is the profile for actuation event
        public ActuationEvent(double _OnTime,double _OffTime,double _OnDuration){
            this.OnTime=_OnTime;
            this.OffTime=_OffTime;
            this.OnDuration=_OnDuration;
        }
        public double OnTime;
        public double OffTime;
        public double OnDuration;
    }

    public static class ActuationEventByDetector{
        // This is the profile for actuation event by detector
        public ActuationEventByDetector(int _DetectorID,double _StartTime,double _EndTime,List<ActuationEvent> _actuationEventList){
            this.DetectorID=_DetectorID;
            this.StartTime=_StartTime;
            this.EndTime=_EndTime;
            this.actuationEventList=_actuationEventList;
        }
        public int DetectorID;
        public double StartTime;
        public double EndTime;
        public List<ActuationEvent> actuationEventList;
    }

    public static class FlowOccByDetector{
        // This is the flow-occ profile by detector
        public FlowOccByDetector(int _DetectorID,double _StartTime, double _EndTime,
                                 double _AggregatedInterval, double[][] _TimeFlowOcc){
         this.DetectorID=_DetectorID;
         this.StartTime=_StartTime;
         this.EndTime=_EndTime;
         this.AggregatedInterval=_AggregatedInterval;
         this.TimeFlowOcc=_TimeFlowOcc;
        }
        public int DetectorID;
        public double StartTime;
        public double EndTime;
        public double AggregatedInterval;
        public double[][] TimeFlowOcc; // Row: (EndTime-StartTime)/AggregatedInterval
    }

    public static class ChangeToGreenYellowRedTimeSeries{
        // This is the profile of time series that change to Green, Yellow & red
        public ChangeToGreenYellowRedTimeSeries(List<Double> _ReferenceTime,List<double []> _ChangeToGreen
                , List<double []> _ChangeToYellow, List<double []> _ChangeToRed){
            this.ReferenceTime=_ReferenceTime;
            this.ChangeToGreen=_ChangeToGreen;
            this.ChangeToYellow=_ChangeToYellow;
            this.ChangeToRed=_ChangeToRed;
        }
        public List<Double> ReferenceTime;
        public List<double []> ChangeToGreen;
        public List<double []> ChangeToYellow;
        public List<double []> ChangeToRed;
    }

    public static List<ActuationEventByDetector> getDetectorActuationEventsForGivenDateAndTimePeriod(int Date
            , double FromTime, double ToTime,String IntIPStr, Connection con){
        // This function is used to get detector actuation events for given date and time period

        List<ActuationEventByDetector> actuationEventByDetectorList=new ArrayList<ActuationEventByDetector>();

        int Year=Date/10000;
        String DataTable=IntIPStr+"_"+Year; // get the right table
        try{
            Statement ps=con.createStatement();
            String sql="Select Time,EventID,EventValue From "+ DataTable + " Where Date="+Date + " And Time>="+FromTime
                    + " And Time<="+ToTime + " And EventID>=81 And EventID<=82 order by Time;"; // Detector "on" and "off" events, order by time
            ResultSet resultSet=ps.executeQuery(sql);
            EventProperties eventProperties=getEventPropertiesFromResultSet(resultSet); // For all detectors
            List<Integer> uniqueDetectors=getUniqueDetectorsFromEventProperties(eventProperties); // Get the unique set of detectors
            for (int i=0;i<uniqueDetectors.size();i++){
                // Loop for each unique detector
                ActuationEventByDetector actuationEventByDetector=getActuationEventByDetector(eventProperties
                        , uniqueDetectors.get(i),FromTime,ToTime); // For individual detectors
                actuationEventByDetectorList.add(actuationEventByDetector);// Create a list
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return actuationEventByDetectorList;
    }

    public static List<ActuationEventByDetector> getActuationEventsForGivenDateAndTimePeriodAndDetectorList(int Date
            , double FromTime, double ToTime,String IntIPStr, Connection con,List<Integer> DetectorList){
        // This function is used to get actuation events for given date and time period and detector list

        List<ActuationEventByDetector> actuationEventByDetectorList=new ArrayList<ActuationEventByDetector>();

        int Year=Date/10000;
        String DataTable=IntIPStr+"_"+Year; // get the right table
        try{
            Statement ps=con.createStatement();
            String EventValueStr=" And (";
            for(int j=0;j<DetectorList.size();j++) {
                EventValueStr=EventValueStr+" EventValue="+DetectorList.get(j);
                if(j<DetectorList.size()-1){
                    EventValueStr=EventValueStr+" OR ";
                }else{
                    EventValueStr=EventValueStr+" ) ";
                }
            }
            String sql = "Select Time,EventID,EventValue From " + DataTable + " Where Date=" + Date + " And Time>=" + FromTime
                    + " And Time<=" + ToTime + " And EventID>=81 And EventID<=82 "+EventValueStr+" order by Time;"; // Detector "on" and "off" events, order by time
            ResultSet resultSet = ps.executeQuery(sql);
            EventProperties eventProperties = getEventPropertiesFromResultSet(resultSet);
            List<Integer> uniqueDetectors = getUniqueDetectorsFromEventProperties(eventProperties); // Get the unique set of detectors
            for (int i = 0; i < uniqueDetectors.size(); i++) {
                // Loop for each unique detector
                ActuationEventByDetector actuationEventByDetector = getActuationEventByDetector(eventProperties
                        , uniqueDetectors.get(i), FromTime, ToTime);
                actuationEventByDetectorList.add(actuationEventByDetector);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return actuationEventByDetectorList;
    }

    public static ActuationEventByDetector getActuationEventByDetector(EventProperties eventProperties, int DetectorID
            ,double StartTime,double EndTime){
        // This function is used to get actuation events for a given detector
        List<ActuationEvent> actuationEventList=new ArrayList<ActuationEvent>();
        List<Double> eventTimes=eventProperties.eventTimes;
        List<Integer> eventIDs=eventProperties.eventIDs;
        List<Integer> eventValues=eventProperties.eventValues;

        // Get the events belonging to the same detector ID
        List<double[]> selectedTimes= new ArrayList<double[]>(); // double [time, event ID];
        for(int i=0;i<eventTimes.size();i++){
            if(eventValues.get(i)==DetectorID){
                selectedTimes.add(new double[]{eventTimes.get(i),eventIDs.get(i)});
            }
        }

        double OnTime,OffTime,OnDuaration;
        int addressOffTime=0;
        // In this method, we assume First-In-First-Out & the on/off times are sorted
        for(int i=0;i<selectedTimes.size();i++){
            if(selectedTimes.get(i)[1]==81 && i==0){ // The first event is "off" event
                OnTime=StartTime;
                OffTime=selectedTimes.get(i)[0];
                OnDuaration=OffTime-OnTime;
                ActuationEvent actuationEvent=new ActuationEvent(OnTime, OffTime,OnDuaration);
                actuationEventList.add(actuationEvent);
            }else if(selectedTimes.get(i)[1]==82 && i==selectedTimes.size()-1){ // The last event is "on" event
                OnTime=selectedTimes.get(i)[0];
                OffTime=EndTime;
                OnDuaration=OffTime-OnTime;
                ActuationEvent actuationEvent=new ActuationEvent(OnTime, OffTime,OnDuaration);
                actuationEventList.add(actuationEvent);
            }else{
                if(selectedTimes.get(i)[1]==82 && i!=0){// If it is an "on" Event
                    OnTime=selectedTimes.get(i)[0];
                    for(int j=addressOffTime;j<selectedTimes.size();j++){ // Search the corresponding "Off" event
                        if(selectedTimes.get(j)[1]==81 && selectedTimes.get(j)[0]>=OnTime){
                            OffTime=selectedTimes.get(j)[0];
                            OnDuaration=OffTime-OnTime;
                            ActuationEvent actuationEvent=new ActuationEvent(OnTime, OffTime,OnDuaration);
                            actuationEventList.add(actuationEvent);
                            addressOffTime=j;
                            break;
                        }
                    }
                }
            }
        }
        ActuationEventByDetector actuationEventByDetector=new ActuationEventByDetector(DetectorID,
                StartTime,EndTime,actuationEventList);
        return actuationEventByDetector;
    }

    public static List<Integer> getUniqueDetectorsFromEventProperties(EventProperties eventProperties){
        // This function is used to get unique detectors within the event properties
        List<Integer> uniqueDetectors=new ArrayList<Integer>();
        HashSet<Integer> hashSet=new HashSet<Integer>();

        for(int i=0;i<eventProperties.eventValues.size();i++){
            if(hashSet.add(eventProperties.eventValues.get(i))){
                uniqueDetectors.add(eventProperties.eventValues.get(i));
            }
        }
        return uniqueDetectors;
    }

    public static List<FlowOccByDetector> getFlowOccForGivenDateAndTimePeriodAndInterval(
            List<ActuationEventByDetector> actuationEventByDetectorList,double Interval,List<Double> NumOfLanes){
        // This function is used to get the flow-occ for a given date and time period and interval
        List<FlowOccByDetector> flowOccByDetectorList=new ArrayList<FlowOccByDetector>();
        for(int i=0;i<actuationEventByDetectorList.size();i++){
            ActuationEventByDetector actuationEventByDetector=actuationEventByDetectorList.get(i);
            int DetectorID=actuationEventByDetector.DetectorID;
            double StartTime=actuationEventByDetector.StartTime;
            double EndTime=actuationEventByDetector.EndTime;
            List<ActuationEvent> actuationEventList=actuationEventByDetector.actuationEventList;
            double lanes=NumOfLanes.get(i);

            int NumOfIntervals=(int) Math.floor((EndTime-StartTime)/Interval)+1;
            double [][] TimeFlowOcc=new double[NumOfIntervals][3];
            for(int j=0;j<NumOfIntervals;j++){
                TimeFlowOcc[j][0]=(StartTime+j*Interval);
                TimeFlowOcc[j][1]=0;
                TimeFlowOcc[j][2]=0;
            }
            //Update flow-rate
            // Check whether the Off-time is within the defined period
            for(int j=0;j<actuationEventList.size();j++){
                double OffTime=actuationEventList.get(j).OffTime;
                int Index=(int)Math.floor((OffTime-StartTime)/Interval)+1;// Find the right address
                if(Index>0 && Index<=NumOfIntervals){ // Within the defined period
                    TimeFlowOcc[Index-1][1]=TimeFlowOcc[Index-1][1]+1;
                }
            }
            //Update occupancy
            for(int j=0;j<actuationEventList.size();j++){
                double OnTime=actuationEventList.get(j).OnTime;
                double OffTime=actuationEventList.get(j).OffTime;
                int IndexOn=(int)Math.floor((OnTime-StartTime)/Interval)+1;
                int IndexOff=(int)Math.floor((OffTime-StartTime)/Interval)+1;
                // Rescaling
                if(IndexOn<=NumOfIntervals && IndexOff>0) {
                    if (IndexOn < 1) {
                        IndexOn = 1;
                        OnTime = StartTime;
                    }

                    if (IndexOff > NumOfIntervals) {
                        IndexOff = NumOfIntervals;
                        OffTime = EndTime;
                    }
                    // Update
                    if(IndexOn==IndexOff){
                        TimeFlowOcc[IndexOff-1][2]=TimeFlowOcc[IndexOff-1][2]+(OffTime-OnTime);
                    }else{
                        TimeFlowOcc[IndexOn - 1][2] = TimeFlowOcc[IndexOn - 1][2] + (IndexOn*Interval+StartTime-OnTime);
                        TimeFlowOcc[IndexOff - 1][2] = TimeFlowOcc[IndexOff - 1][2] + (OffTime - (IndexOff - 1) * Interval-StartTime);
                        if(IndexOff-IndexOn>1) {
                            for (int k = IndexOn; k < IndexOff - 1; k++) {
                                TimeFlowOcc[k][2] = TimeFlowOcc[k][2] + Interval;
                            }
                        }
                    }

                }
            }
            // Finalizing
            for(int j=0;j<NumOfIntervals;j++){
                TimeFlowOcc[j][1]=TimeFlowOcc[j][1]*3600/Interval;
                TimeFlowOcc[j][2]=TimeFlowOcc[j][2]/Interval/lanes*100;
            }

            FlowOccByDetector flowOccByDetector=new FlowOccByDetector(DetectorID,StartTime,EndTime,Interval,TimeFlowOcc);
            flowOccByDetectorList.add(flowOccByDetector);
        }
        return flowOccByDetectorList;
    }

    public static double [][] calulateArrivalOnRedFlowForGivenPhaseAndTimePeriod(List<PhaseOnOffEvents> phaseOnOffEventsList,
            List<ActuationEventByDetector> actuationEventByDetectorList,double FromTime, double ToTime, double Interval,double MaxLatency){
        // This function is used to calculate the metric of Arrival On Red

        // Get the number of intervals
        int NumOfIntervals=(int) Math.floor((ToTime-FromTime)/Interval)+1;
        double [][] arrivalOnRedFlow= new double[NumOfIntervals][5];
        for(int i=0;i<NumOfIntervals;i++){
            arrivalOnRedFlow[i][0]=FromTime+i*Interval;
            arrivalOnRedFlow[i][1]=FromTime+(i+1)*Interval;
            arrivalOnRedFlow[i][2]=0; // Arrival-on-red flow
            arrivalOnRedFlow[i][3]=0; // Arrival flow
            arrivalOnRedFlow[i][4]=0;// Percentage
        }

        for(int i=0;i<actuationEventByDetectorList.size();i++){
            List<ActuationEvent> actuationEventList=actuationEventByDetectorList.get(i).actuationEventList;
            for(int j=0;j<actuationEventList.size();j++){
                int AddressEvent=(int)Math.floor(actuationEventList.get(j).OffTime/Interval);
                // Get the approach flow
                arrivalOnRedFlow[AddressEvent][3]=arrivalOnRedFlow[AddressEvent][3]+1;
                // Get the arrival-on-red flow
                boolean Status=CheckWhetherArrivalOnRed(actuationEventList.get(j),phaseOnOffEventsList,MaxLatency);
                if(Status){
                    arrivalOnRedFlow[AddressEvent][2]=arrivalOnRedFlow[AddressEvent][2]+1;
                }
            }
        }

        // Update the percentage
        double ConvertToHourlyFlow=3600.0/Interval;
        for(int i=0;i<NumOfIntervals;i++){
            if(arrivalOnRedFlow[i][3]!=0) {
                arrivalOnRedFlow[i][2]=arrivalOnRedFlow[i][2]*ConvertToHourlyFlow;
                arrivalOnRedFlow[i][3]=arrivalOnRedFlow[i][3]*ConvertToHourlyFlow;
                arrivalOnRedFlow[i][4] = arrivalOnRedFlow[i][2]/arrivalOnRedFlow[i][3]*100;// Percentage
            }
        }
        return arrivalOnRedFlow;
    }

    public static boolean CheckWhetherArrivalOnRed(ActuationEvent actuationEvent,List<PhaseOnOffEvents> phaseOnOffEventsList,double MaxLatency){
        // Check whether an actuation event (an arrival) is during the red time

        boolean Status=false;
        double EventOffTime=actuationEvent.OffTime;
        double ProjectedArrivalTime=EventOffTime+MaxLatency;
        for(int i=1;i<phaseOnOffEventsList.size();i++){ // Start from the second row
            // If it is within the red period
            if(phaseOnOffEventsList.get(i-1).OffTime<ProjectedArrivalTime && phaseOnOffEventsList.get(i).OnTime>ProjectedArrivalTime){
                // Previous phase offtime and next phase on time
                Status=true;
                break;
            }
        }
        return Status;
    }

    public static ChangeToGreenYellowRedTimeSeries GetTimeSeriesChangeToGreenYellowRed(PhasePropertyByID curPhaseProperty
            ,PhasePropertyByID prePhaseProperty){
        // This function is used to get time series that change to green, yellow, and red

        List<double []> ChangeToGreen=new ArrayList<double[]>();
        List<double []> ChangeToYellow=new ArrayList<double[]>();
        List<double []> ChangeToRed=new ArrayList<double[]>();
        List<Double> ReferenceTimeList=new ArrayList<Double>();

        for(int i=0;i<curPhaseProperty.phasePropertyList.size();i++){
            double TimeToGreen=curPhaseProperty.phasePropertyList.get(i).GreenStartTime;
            double TimeToYellow=curPhaseProperty.phasePropertyList.get(i).YellowStartTime;
            double TimeToRed=curPhaseProperty.phasePropertyList.get(i).AllRedStartTime;

            if(TimeToRed>=TimeToYellow && TimeToYellow>=TimeToGreen && TimeToGreen>0){
                double ReferenceTime=TimeToGreen; // Offset is zero in this case
                if(prePhaseProperty!=null){
                    ReferenceTime=CheckPhaseOffset(TimeToGreen,prePhaseProperty);
                }
                ReferenceTimeList.add(ReferenceTime);
                ChangeToGreen.add(new double[]{ReferenceTime/3600.0,(TimeToGreen-ReferenceTime)});
                ChangeToYellow.add(new double[]{ReferenceTime/3600.0,(TimeToYellow-ReferenceTime)});
                ChangeToRed.add(new double[]{ReferenceTime/3600.0,(TimeToRed-ReferenceTime)});
            }else{
                System.out.println("Invalid information for Phase"+curPhaseProperty.PhaseID+
                        ": Green:"+TimeToGreen/3600.0+ "& Yellow:"+ TimeToYellow/3600.0+" & Red:"+ TimeToRed/3600.0);
            }
        }
        ChangeToGreenYellowRedTimeSeries changeToGreenYellowRedTimeSeries=new ChangeToGreenYellowRedTimeSeries(ReferenceTimeList,ChangeToGreen
                ,ChangeToYellow, ChangeToRed);
        return changeToGreenYellowRedTimeSeries;
    }

    public static double CheckPhaseOffset(double TimeToGreen,PhasePropertyByID prePhaseProperty){
        // This function is used to get the phase offset (reference point from the beginning of the phase)

        double ReferenceTime=TimeToGreen;
        double MaximumGap=180; // A maximum gap should be within an acceptable range, e.g. the maximum cycle length in prevailing settings
        for(int i=0;i<prePhaseProperty.phasePropertyList.size();i++){
            double PreTimeToGreen=prePhaseProperty.phasePropertyList.get(i).GreenStartTime;
            if(PreTimeToGreen<=TimeToGreen && (TimeToGreen-PreTimeToGreen)<MaximumGap){ // If it is smaller than the max gap
                MaximumGap=TimeToGreen-PreTimeToGreen; // Update the gap
                ReferenceTime=PreTimeToGreen; // Update the reference time
            }
        }
        return ReferenceTime;
    }

    public static List<double []> CheckDetectorEventOffset(List<ActuationEventByDetector> actuationEventByDetectorList,List<Double> ReferenceTime){
        // This function is used to check detector event offsets

        List<double []> DetectorEventOffset =new ArrayList<double[]>(); //[Reference time, time in a cycle]
        Collections.sort(ReferenceTime); // Sort the reference time
        for(int i=0;i<actuationEventByDetectorList.size();i++){ // Loop for each detector ID
            for(int k=0;k<actuationEventByDetectorList.get(i).actuationEventList.size();k++) {// Loop for the actuation events for each detector
                double OnTime = actuationEventByDetectorList.get(i).actuationEventList.get(k).OnTime; // Get the On-Time
                for (int j = 1; j < ReferenceTime.size(); j++) { // Starting from index=1
                    if (OnTime < ReferenceTime.get(j) && OnTime >= ReferenceTime.get(j-1) ) { // Find it is in-between
                        DetectorEventOffset.add(new double[]{ReferenceTime.get(j-1) / 3600.0, OnTime - ReferenceTime.get(j-1)});
                        break;
                    }
                }
            }
        }
        return DetectorEventOffset;
    }

    //******************************************************************************
    //******************************Pedestrian Events ********************************
    //******************************************************************************
    public static class PedestrianCall{
        // This is the profile for pedestrian calls
        public PedestrianCall(double _OnTime, double _OffTime){
            this.OnTime=_OnTime;
            this.OffTime=_OffTime;
        }
        public double OnTime;
        public double OffTime;
    }

    public static class PhaseOnOffEvents{
        // This is the profile to record phase on/off events
        public PhaseOnOffEvents(double _OnTime,double _OffTime){
            this.OnTime=_OnTime;
            this.OffTime=_OffTime;
        }
        public double OnTime;
        public double OffTime;
    }

    public static class PedestrianCallByPhase{
        // This is the profile for pedestrian call by phase
        public PedestrianCallByPhase(int _PhaseID, List<PedestrianCall> _pedestrianCallList, List<PhaseOnOffEvents> _phaseOnOffEventsList){
            this.PhaseID=_PhaseID;
            this.pedestrianCallList=_pedestrianCallList;
            this.phaseOnOffEventsList=_phaseOnOffEventsList;
        }
        public int PhaseID;
        public List<PedestrianCall> pedestrianCallList; // Ordered by time
        public List<PhaseOnOffEvents> phaseOnOffEventsList; // Ordered by Time
    }

    public static class WaitingTimeForPedestrianCall{
        // This is the waiting times for pedestrian calls
        public WaitingTimeForPedestrianCall(double _PedOnTime, double _PhaseOnTime, double _PhaseOffTime, double _WaitingTime){
            this.PedOnTime=_PedOnTime;
            this.PhaseOnTime=_PhaseOnTime;
            this.PhaseOffTime=_PhaseOffTime;
            this.WaitingTime=_WaitingTime;
        }
        public double PedOnTime; // Pedestrian arrival time
        public double PhaseOnTime; // Next Phase start time
        public double PhaseOffTime; // Next phase end time
        public double WaitingTime; //Pedestrian waiting time
    }

    public static class TotalPedCallAndAvgWaitingTimeByInterval{
        // This is the aggregated metrics: total ped calls, avg waiting time, and max waiting time
        public TotalPedCallAndAvgWaitingTimeByInterval(double [] _StartTime, double [] _EndTime, double [] _AvgWaitingTime,
                                                       double [] _MaxWaitingTime,double [] _TotalPedCalls){
            this.StartTime=_StartTime;
            this.EndTime=_EndTime;
            this.AvgWaitingTime=_AvgWaitingTime;
            this.MaxWaitingTime=_MaxWaitingTime;
            this.TotalPedCalls=_TotalPedCalls;
        }
        public double [] StartTime;
        public double [] EndTime;
        public double [] AvgWaitingTime; // Series of avg waiting times
        public double [] MaxWaitingTime; // Series of max waiting times
        public double [] TotalPedCalls; // Series of total pedestrian calls
    }

    public static List<PedestrianCall> getPedDetectorEventsByPhaseForGivenDateAndTimePeriod(int PhaseID,int Date
            , double FromTime, double ToTime,String IntIPStr, Connection con){
        // This function is used to get pedestrian detector events by phase ID for given date and time period

        List<PedestrianCall> pedestrianCallList=new ArrayList<PedestrianCall>();

        int Year=Date/10000;
        String DataTable=IntIPStr+"_"+Year; // get the right table
        try{
            Statement ps=con.createStatement();
            String sql="Select Time,EventID,EventValue From "+ DataTable + " Where Date="+Date + " And Time>="+FromTime
                    + " And Time<="+ToTime + " And EventID>=89 And EventID<=90 And EventValue="+PhaseID+" order by Time;";
            ResultSet resultSet=ps.executeQuery(sql);
            EventProperties eventProperties=getEventPropertiesFromResultSet(resultSet);

            int CurOffEvent=0;
            for(int i=0;i<eventProperties.eventTimes.size();i++){ // Loop for each event
                if(eventProperties.eventIDs.get(i)==90) // Event On
                {
                    double OnTime=eventProperties.eventTimes.get(i); // Get the on-time
                    for(int j=CurOffEvent+1;j<eventProperties.eventTimes.size();j++){// Loop for the next lines
                        if(eventProperties.eventIDs.get(j)==89) // Event Off
                        {
                            double OffTime=eventProperties.eventTimes.get(j); // Get the off-time
                            PedestrianCall pedestrianCall=new PedestrianCall(OnTime,OffTime);
                            pedestrianCallList.add(pedestrianCall);
                            CurOffEvent=j; // Adjust the position of current off event
                            break;
                        }
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return pedestrianCallList;
    }

    public static List<PhaseOnOffEvents> getPhaseOnOffEventsForGivenDateAndTimePeriod(int PhaseID,int Date
            , double FromTime, double ToTime,String IntIPStr, Connection con){
        // This function is used to get phase on/off events for given date and time period

        List<PhaseOnOffEvents> phaseOnOffEventsList=new ArrayList<PhaseOnOffEvents>();

        int Year=Date/10000;
        String DataTable=IntIPStr+"_"+Year; // get the right table
        try{
            Statement ps=con.createStatement();
            String sql="Select Time,EventID,EventValue From "+ DataTable + " Where Date="+Date + " And Time>="+FromTime
                    + " And Time<="+ToTime + " And EventID>=0 And EventID<=20 And EventValue="+PhaseID+" order by Time;";
            ResultSet resultSet=ps.executeQuery(sql);
            EventProperties eventProperties=getEventPropertiesFromResultSet(resultSet);

            int CurOffEvent=0;
            for(int i=0;i<eventProperties.eventTimes.size();i++){ // Loop for each event
                if(eventProperties.eventIDs.get(i)==0) // Event On
                {
                    double OnTime=eventProperties.eventTimes.get(i); // Get the on-time
                    for(int j=CurOffEvent+1;j<eventProperties.eventTimes.size();j++){// Loop for the next lines
                        if(eventProperties.eventIDs.get(j)==12 && eventProperties.eventTimes.get(j)>OnTime) // Event Off
                        {
                            double OffTime=eventProperties.eventTimes.get(j); // Get the off-time
                            PhaseOnOffEvents phaseOnOffEvents=new PhaseOnOffEvents(OnTime,OffTime);
                            phaseOnOffEventsList.add(phaseOnOffEvents);
                            CurOffEvent=j; // Adjust the position of current off event
                            break;
                        }
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return phaseOnOffEventsList;
    }

    public static PedestrianCallByPhase getPedestrianCallByPhase(int PhaseID,int Date
            , double FromTime, double ToTime,String IntIPStr, Connection con){
        // This function is used to get pedestrian calls by phase

        List<PedestrianCall> pedestrianCallList=getPedDetectorEventsByPhaseForGivenDateAndTimePeriod(PhaseID,Date
            , FromTime, ToTime,IntIPStr, con);

        List<PhaseOnOffEvents> phaseOnOffEventsList=getPhaseOnOffEventsForGivenDateAndTimePeriod(PhaseID,Date
                , FromTime, ToTime,IntIPStr, con);

        PedestrianCallByPhase pedestrianCallByPhase=new PedestrianCallByPhase(PhaseID,pedestrianCallList,phaseOnOffEventsList);
        return pedestrianCallByPhase;
    }

    public static List<WaitingTimeForPedestrianCall> calculateWaitingTimeForPedestrianCalls(PedestrianCallByPhase pedestrianCallByPhase){
        // This function is used to calculate the waiting times for pedestrian calls
        List<WaitingTimeForPedestrianCall> waitingTimeForPedestrianCallList=new ArrayList<WaitingTimeForPedestrianCall>();

        List<PedestrianCall> pedestrianCallList=pedestrianCallByPhase.pedestrianCallList; // Ordered by Time
        List<PhaseOnOffEvents> phaseOnOffEventsList=pedestrianCallByPhase.phaseOnOffEventsList; // Ordered by Time

        for(int i=0;i<pedestrianCallList.size();i++){ // Loop for each pedestrian call
            double PedOnTime=pedestrianCallList.get(i).OnTime; // Get the ped on time
            for(int j=0;j<phaseOnOffEventsList.size();j++){ // Loop for each phase on/off events
                if(phaseOnOffEventsList.get(j).OnTime>=PedOnTime){ // For the first phase on time greater than the PedOnTime
                    // Pedestrian calls will occur when the current green phase is going to terminate. Thus the waiting time is very long
                    double PhaseOnTime=phaseOnOffEventsList.get(j).OnTime;
                    double PhaseOffTime=phaseOnOffEventsList.get(j).OffTime;
                    double WaitingTime=PhaseOnTime-PedOnTime; // Get the waiting time
                    WaitingTimeForPedestrianCall waitingTimeForPedestrianCall=new WaitingTimeForPedestrianCall(PedOnTime
                            , PhaseOnTime,PhaseOffTime, WaitingTime);
                    waitingTimeForPedestrianCallList.add(waitingTimeForPedestrianCall);
                    break;
                }
            }
        }
        return waitingTimeForPedestrianCallList;
    }

    public static TotalPedCallAndAvgWaitingTimeByInterval getTotalPedCallAndAvgWaitingTimeByInterval(
            List<WaitingTimeForPedestrianCall> waitingTimeForPedestrianCallList, double FromTime,double ToTime, double Interval){
        // This function is used to get total pedestrian calls and average waiting time by interval

        // Construct the matrix
        int TotalInterval=(int) Math.ceil((ToTime-FromTime)/Interval);
        double [] StartTime =new double[TotalInterval];
        double [] EndTime =new double[TotalInterval];
        double [] AvgWaitingTime =new double[TotalInterval];
        double [] MaxWaitingTime= new double[TotalInterval];
        double [] TotalPedCalls= new double[TotalInterval];
        for(int i=0;i<TotalInterval;i++){
            StartTime[i]=FromTime+i*Interval;
            EndTime[i]=FromTime+(i+1)*Interval;
            AvgWaitingTime[i]=0;
            MaxWaitingTime[i]=0;
            TotalPedCalls[i]=0;
        }
        // Get the total pedestrian calls
        for(int i=0;i<waitingTimeForPedestrianCallList.size();i++){
            double time=waitingTimeForPedestrianCallList.get(i).PedOnTime;
            int address=(int)Math.floor((time-FromTime)/Interval);
            TotalPedCalls[address]=TotalPedCalls[address]+1.0;
            AvgWaitingTime[address]=AvgWaitingTime[address]+waitingTimeForPedestrianCallList.get(i).WaitingTime;
            if(waitingTimeForPedestrianCallList.get(i).WaitingTime>MaxWaitingTime[address]){
                MaxWaitingTime[address]=waitingTimeForPedestrianCallList.get(i).WaitingTime;
            }
        }
        // Get the average waiting time
        for(int i=0;i<TotalInterval;i++){
            if(TotalPedCalls[i]>0){
                AvgWaitingTime[i]=AvgWaitingTime[i]/TotalPedCalls[i];
            }
        }

        TotalPedCallAndAvgWaitingTimeByInterval totalPedCallAndAvgWaitingTimeByInterval=new TotalPedCallAndAvgWaitingTimeByInterval(
                StartTime,EndTime, AvgWaitingTime,MaxWaitingTime,TotalPedCalls);
        return totalPedCallAndAvgWaitingTimeByInterval;
    }


    //********************************************************************************
    //******************************Drawing Functions ********************************
    //********************************************************************************
    // Split Monitor
    public static void drawSplitMonitor(List<PhasePropertyByID> phasePropertyByIDList,List<PlanProperty> planPropertyList, int Date,
                                        String IntName){
        // This function is used to draw split monitor

        int MinimumPoint=2;
        int NumPhases=phasePropertyByIDList.size(); // Get Number of Phases
        for (int i = 0; i < NumPhases; i++) { // Loop for each phase
            PhasePropertyByID phasePropertyByID=phasePropertyByIDList.get(i);
            int PhaseID=phasePropertyByID.PhaseID;
            List<PhaseProperty> phasePropertyList=phasePropertyByID.phasePropertyList;

            if(phasePropertyList.size()>MinimumPoint) {
                // Get the actual green times
                double[] EndTimes = new double[phasePropertyList.size()];
                double[] ActualGreenTimes = new double[phasePropertyList.size()];
                for (int j = 0; j < phasePropertyList.size(); j++) {
                    EndTimes[j] = phasePropertyList.get(j).GreenEndTime;
                    ActualGreenTimes[j] = phasePropertyList.get(j).GreenDuration;
                }

                XYSeriesCollection DataSet = new XYSeriesCollection();
                if (phasePropertyList.size() > 0) {
                    List<Integer> uniquePlanIDs=findUniquePlanIDs(planPropertyList);
                    for(int j=0;j<uniquePlanIDs.size();j++){
                        final XYSeries seriesPlan = getXTimeYMeasurePlannedRev(planPropertyList,uniquePlanIDs.get(j),PhaseID);
                        seriesPlan.setKey("Plan" + uniquePlanIDs.get(j)+":(Planned)");// Set Key
                        final XYSeries series =getXTimeYMeasureByTimingStatusRev(EndTimes, ActualGreenTimes, planPropertyList,
                                uniquePlanIDs.get(j));
                        if (series != null) {
                            series.setKey("Plan" + uniquePlanIDs.get(j)+":(Actual)");// Set Key
                            DataSet.addSeries(seriesPlan);// Add the series: plan values
                            DataSet.addSeries(series);// Add the series: actual values
                        }
                    }

                    JFreeChart demo = ChartFactory.createScatterPlot("Split Monitor at "+IntName+" for Phase " + PhaseID+" on Date "+Date,
                            "Time (Hour of Day)","Duration (Second)", DataSet, PlotOrientation.VERTICAL, true, true, false);
                    final ChartPanel chartPanel = new ChartPanel(demo);
                    chartPanel.setPreferredSize(new java.awt.Dimension(1000, 500));
                    final ApplicationFrame frame = new ApplicationFrame("Split Monitor");
                    frame.setContentPane(chartPanel);
                    frame.pack();
                    frame.setVisible(true);
                    LegendTitle legend = demo.getLegend();
                    legend.setPosition(RectangleEdge.RIGHT);

                    XYPlot xyPlot = (XYPlot) demo.getPlot();
                    xyPlot.setDomainCrosshairVisible(true);
                    xyPlot.setRangeCrosshairVisible(true);
                    XYItemRenderer renderer = xyPlot.getRenderer();
                    renderer.setSeriesPaint(0, Color.blue);
                    NumberAxis domain = (NumberAxis) xyPlot.getDomainAxis();
                    domain.setRange(0.00, 24.00);
                    NumberAxis range = (NumberAxis) xyPlot.getRangeAxis();
                    range.setRange(0.0, 120.0);


                    XYLineAndShapeRenderer rendererLineShape = new XYLineAndShapeRenderer();
                    for(int j=0;j<xyPlot.getSeriesCount()/2;j++){
                        // sets thickness for series (using strokes)
                        double size=3;
                        rendererLineShape.setSeriesShape(2*j,new Rectangle.Double(-size/2, -size/2, size, size));
                        rendererLineShape.setSeriesLinesVisible(2*j,false);

                        rendererLineShape.setSeriesShape(2*j+1,ShapeUtilities.createDiamond((int)size));
                        rendererLineShape.setSeriesLinesVisible(2*j+1, false);
                    }
                    xyPlot.setRenderer(0,rendererLineShape);
                }
            }
        }
    }

    // Purdue Phase Termination
    public static void drawPurduePhaseTermination(List<PhasePropertyByID> phasePropertyByIDList,List<PlanProperty> planPropertyList,
                                                  int MaxNumPhase,int Date,String IntName){
        // This function is used to draw purdue phase termination
        int MinimumPoint=2;
        List<double []> GapOutTime=new ArrayList<double []>();
        List<double []> MaxOutTime=new ArrayList<double []>();
        List<double []> ForceOffTime=new ArrayList<double []>();
        for(int i=1;i<=MaxNumPhase; i++){// Loop for each phase
            for(int j=0;j<phasePropertyByIDList.size();j++){
                if(phasePropertyByIDList.get(j).PhaseID==i){// Find the same phase
                    int PhaseID=phasePropertyByIDList.get(j).PhaseID;
                    List<PhaseProperty> phasePropertyList=phasePropertyByIDList.get(j).phasePropertyList;
                    if(phasePropertyList.size()>MinimumPoint){
                        for(int k=0;k<phasePropertyList.size();k++){// Loop for each cycle
                            if(phasePropertyList.get(k).IsGapOut){
                                GapOutTime.add(new double []{phasePropertyList.get(k).GreenEndTime/3600.0,PhaseID});
                            }
                            if(phasePropertyList.get(k).IsMaxOut){
                                MaxOutTime.add(new double []{phasePropertyList.get(k).GreenEndTime/3600.0,PhaseID});
                            }
                            if(phasePropertyList.get(k).IsForceOff){
                                ForceOffTime.add(new double []{phasePropertyList.get(k).GreenEndTime/3600.0,PhaseID});
                            }
                        }
                    }
                    break;
                }
            }
        }

        XYSeriesCollection DataSet = new XYSeriesCollection();
        if(GapOutTime.size()>0) {
            XYSeries xySeriesGapOut = new XYSeries(GapOutTime.size());
            for(int i=0;i<GapOutTime.size();i++){
                xySeriesGapOut.add(GapOutTime.get(i)[0],GapOutTime.get(i)[1]);
            }
            xySeriesGapOut.setKey("Gap Out");
            DataSet.addSeries(xySeriesGapOut);
        }
        if(MaxOutTime.size()>0) {
            XYSeries xySeriesMaxOut=new XYSeries(MaxOutTime.size());
            for(int i=0;i<MaxOutTime.size();i++){
                xySeriesMaxOut.add(MaxOutTime.get(i)[0],MaxOutTime.get(i)[1]);
            }
            xySeriesMaxOut.setKey("Max Out");
            DataSet.addSeries(xySeriesMaxOut);
        }
        if(ForceOffTime.size()>0) {
            XYSeries xySeriesForceOff=new XYSeries(ForceOffTime.size());
            for(int i=0;i<ForceOffTime.size();i++){
                xySeriesForceOff.add(ForceOffTime.get(i)[0],ForceOffTime.get(i)[1]);
            }
            xySeriesForceOff.setKey("Force Off");
            DataSet.addSeries(xySeriesForceOff);
        }

        List<Integer> uniquePlanIDs=findUniquePlanIDs(planPropertyList);

        for(int j=0;j<uniquePlanIDs.size();j++){
            List<double[]> planTime=new ArrayList<double[]>();
            for(int p=0;p<planPropertyList.size();p++){
                if(uniquePlanIDs.get(j)==planPropertyList.get(p).PlanID){
                    int MaxPoint=100;
                    XYSeries xySeries=new XYSeries(MaxPoint+1); // Get a new xy series
                    double StartTime=planPropertyList.get(p).StartTime;
                    double EndTime=planPropertyList.get(p).EndTime;
                    double DeltaTime=(EndTime-StartTime)/MaxPoint;
                    planTime.add(new double[]{StartTime/3600.0,MaxNumPhase+0.5});
                    for(int i=0;i<MaxPoint;i++){
                        planTime.add(new double[]{(StartTime+(i+1)*DeltaTime)/3600.0,MaxNumPhase+0.5});
                    }
                }
            }
            if(planTime.size()>0){
                XYSeries xySeries=new XYSeries(planTime.size());
                for(int k=0;k<planTime.size();k++){
                    xySeries.add(planTime.get(k)[0],planTime.get(k)[1]);
                }
                xySeries.setKey("Plan" + uniquePlanIDs.get(j));// Set Key
                DataSet.addSeries(xySeries);
            }

        }

        JFreeChart demo = ChartFactory.createScatterPlot("Purdue Phase Termination at "+IntName+" on Date "+Date,
                "Time (Hour of Day)","Phase Number", DataSet, PlotOrientation.VERTICAL, true, true, false);
        final ChartPanel chartPanel = new ChartPanel(demo);
        chartPanel.setPreferredSize(new java.awt.Dimension(1000, 500));
        final ApplicationFrame frame = new ApplicationFrame("Purdue Phase Termination");
        frame.setContentPane(chartPanel);
        frame.pack();
        frame.setVisible(true);
        LegendTitle legend = demo.getLegend();
        legend.setPosition(RectangleEdge.RIGHT);

        XYPlot xyPlot = (XYPlot) demo.getPlot();
        xyPlot.setDomainCrosshairVisible(true);
        xyPlot.setRangeCrosshairVisible(true);
        XYItemRenderer renderer = xyPlot.getRenderer();
        NumberAxis domain = (NumberAxis) xyPlot.getDomainAxis();
        domain.setRange(0.00, 24.00);
        NumberAxis range = (NumberAxis) xyPlot.getRangeAxis();
        range.setRange(0.0, MaxNumPhase+1);
        range.setTickUnit(new NumberTickUnit(1));

        XYLineAndShapeRenderer rendererLineShape = new XYLineAndShapeRenderer();
        Shape Cross = ShapeUtilities.createRegularCross(2, 2);
        rendererLineShape.setSeriesShape(0,Cross);
        Shape Triangle = ShapeUtilities.createUpTriangle(2);
        rendererLineShape.setSeriesShape(1,Triangle);
        Shape Diamond = ShapeUtilities.createDiamond(2);
        rendererLineShape.setSeriesShape(2,Diamond);

        for(int j=0;j<xyPlot.getSeriesCount();j++){
            // sets thickness for series (using strokes)
            if(j<3) {
                rendererLineShape.setSeriesLinesVisible(j, false);
                rendererLineShape.setSeriesShapesFilled(j,false);
            }
            else {
                double size=5;
                rendererLineShape.setSeriesShape(j,new Rectangle.Double(-size/2, -size/2, size, size));
                rendererLineShape.setSeriesLinesVisible(j, false);
            }
        }
        xyPlot.setRenderer(rendererLineShape);

    }

    // Purdue coordination diagram
    public static void drawPurdueCoordinationDiagramByApproach(DetectorByApproach detectorByApproach,int Date, double FromTime, double ToTime,String IntIPStr
            , Connection con, String Title, double Interval){
        // This function is used to draw the purdue coordination diagram

        double SpeedLimit=detectorByApproach.SpeedLimit;
        int PhaseID=detectorByApproach.PhaseForThroughMovement; // Current phase ID
        // Used to calculate the cycle time (the time difference between the beginning of the cycle and the beginning of current phase ID)
        int LeadingPhaseID=detectorByApproach.LeadingPhaseID;
        Title= Title +" : Phase "+PhaseID;
        List<Integer> DetectorList=new ArrayList<Integer>(); // Get the list of advance detectors
        double MaxLatency=0;// Projected time from advance detector to stopbar, in seconds
        for(int i=0;i<detectorByApproach.detectorConfigList.size();i++){
            if(detectorByApproach.detectorConfigList.get(i).Type.equals("AdvanceDetector") &&
                    detectorByApproach.detectorConfigList.get(i).DistanceFromStopbar>0){
                double Latency=(detectorByApproach.detectorConfigList.get(i).DistanceFromStopbar/5280)/SpeedLimit*3600;
                if(Latency>MaxLatency){
                    MaxLatency=Latency;
                }
                DetectorList.add(detectorByApproach.detectorConfigList.get(i).DetectorID);
            }
        }
        // Get the properties of current Phase ID
        PhasePropertyByID phasePropertyByID=getActualPhaseInfForGivenPhaseDateAndTimePeriod(Date,FromTime,ToTime,PhaseID,IntIPStr,con);
        // Get the actuation events for the list of advance detectors belonging to the approach
        List<ActuationEventByDetector> actuationEventByDetectorList=getActuationEventsForGivenDateAndTimePeriodAndDetectorList(Date,FromTime,ToTime,
                IntIPStr, con,DetectorList);

        ChangeToGreenYellowRedTimeSeries changeToGreenYellowRedTimeSeries; // Adjust the phase activities according to the leading phase activities
        if(LeadingPhaseID==PhaseID) {// If the current phase is the leading phase, no action is required
            changeToGreenYellowRedTimeSeries = GetTimeSeriesChangeToGreenYellowRed(phasePropertyByID
                    , null);
        }else{// If it is not!
            PhasePropertyByID leadingPhaseProperty; // Get the leading phase property
            leadingPhaseProperty=getActualPhaseInfForGivenPhaseDateAndTimePeriod(Date,FromTime,ToTime
                    ,LeadingPhaseID,IntIPStr,con);

            changeToGreenYellowRedTimeSeries = GetTimeSeriesChangeToGreenYellowRed(phasePropertyByID
                    , leadingPhaseProperty);
        }

        List<double[]> detectorOnTimes=new ArrayList<double[]>();
        if(changeToGreenYellowRedTimeSeries.ReferenceTime.size()>0) {// Adjust the detector events
            detectorOnTimes=CheckDetectorEventOffset(actuationEventByDetectorList,changeToGreenYellowRedTimeSeries.ReferenceTime);
        }
        drawPurduePhaseCoordinationDiagram(Title,detectorOnTimes, changeToGreenYellowRedTimeSeries);
    }

    public static void drawPurduePhaseCoordinationDiagram(String Title, List<double[]> detectorOnTimes,ChangeToGreenYellowRedTimeSeries changeToGreenYellowRedTimeSeries){
        // This function is used to draw the purdue phase coordination diagram

        XYSeriesCollection dataset = new XYSeriesCollection();

        // Three different lines/series: change to green, change to yellow, and change to red
        List<double[]> ChangeToGreen=changeToGreenYellowRedTimeSeries.ChangeToGreen;
        List<double[]> ChangeToYellow=changeToGreenYellowRedTimeSeries.ChangeToYellow;
        List<double[]> ChangeToRed=changeToGreenYellowRedTimeSeries.ChangeToRed;
        XYSeries xySeriesGreen=new XYSeries(ChangeToGreen.size());
        XYSeries xySeriesYellow=new XYSeries(ChangeToGreen.size());
        XYSeries xySeriesRed=new XYSeries(ChangeToGreen.size());
        for(int i=0;i<ChangeToGreen.size();i++){
            xySeriesGreen.add(ChangeToGreen.get(i)[0],ChangeToGreen.get(i)[1]);
            xySeriesYellow.add(ChangeToYellow.get(i)[0],ChangeToYellow.get(i)[1]);
            xySeriesRed.add(ChangeToRed.get(i)[0],ChangeToRed.get(i)[1]);
        }
        xySeriesGreen.setKey("Change To Green");
        xySeriesYellow.setKey("Change To Yellow");
        xySeriesRed.setKey("Change To Red");
        dataset.addSeries(xySeriesGreen);
        dataset.addSeries(xySeriesYellow);
        dataset.addSeries(xySeriesRed);

        // Detector events
        XYSeries xySeriesDetEvent=new XYSeries(detectorOnTimes.size());
        for(int i=0;i<detectorOnTimes.size();i++){
            xySeriesDetEvent.add(detectorOnTimes.get(i)[0],detectorOnTimes.get(i)[1]);
        }
        xySeriesDetEvent.setKey("Detector Activation");
        dataset.addSeries(xySeriesDetEvent);

        XYPlot plot = new XYPlot();
        plot.setDataset(0, dataset);
        plot.setRenderer(0, new XYSplineRenderer());//use default fill paint for first series
        plot.setRangeAxis(0, new NumberAxis("Cycle Time (Sec)"));
        plot.setDomainAxis(new NumberAxis("Hour of Day (Hr)"));
        plot.mapDatasetToRangeAxis(0, 0);

        XYLineAndShapeRenderer rendererShape = new XYLineAndShapeRenderer();
        rendererShape.setSeriesShapesVisible(0, false);
        rendererShape.setSeriesShapesVisible(1, false);
        rendererShape.setSeriesShapesVisible(2, false);
        rendererShape.setSeriesLinesVisible(3, false);
        rendererShape.setSeriesPaint(0,Color.green);
        rendererShape.setSeriesPaint(1,Color.yellow);
        rendererShape.setSeriesPaint(2,Color.red);
        rendererShape.setSeriesPaint(3,Color.black);
        double size=3;
        rendererShape.setSeriesShape(3,new Rectangle.Double(-size/2, -size/2, size, size));
        rendererShape.setSeriesStroke(0, new BasicStroke(2));
        rendererShape.setSeriesStroke(1, new BasicStroke(2));
        rendererShape.setSeriesStroke(2, new BasicStroke(2));
        plot.setRenderer(0, rendererShape);

        //generate the chart
        JFreeChart chart = new JFreeChart(Title, plot);
        chart.setBackgroundPaint(Color.WHITE);
        final ChartPanel chartPanel = new ChartPanel(chart);
        //setContentPane(chartPanel);
        chartPanel.setPreferredSize(new java.awt.Dimension(1000, 500));
        final ApplicationFrame frame = new ApplicationFrame(Title);
        frame.setContentPane(chartPanel);
        frame.pack();
        frame.setVisible(true);
    }

    // Yellow and Red Actuations
    public static void drawYellowAndRedActuations(List<Integer> DetectorList,int PhaseID, String Movement, int Date
            , double FromTime, double ToTime,String IntIPStr, Connection con, String Title, double Interval){
        // This function is used to draw yellow and all red actuation events

        Title= Title +" : Phase "+PhaseID + " & "+Movement;

        // Get the properties of yellow and all red intervals
        YellowAndAllRedPropertyByID yellowAndAllRedPropertyByID=getActualYellowAndRedInfForGivenPhaseDateAndTimePeriod(Date,FromTime,ToTime,PhaseID,IntIPStr,con);
        // Get the actuation events for the list of advance detectors belonging to the approach
        List<ActuationEventByDetector> actuationEventByDetectorList=getActuationEventsForGivenDateAndTimePeriodAndDetectorList(Date,FromTime,ToTime,
                IntIPStr, con,DetectorList);

        List<double[]> VehicleOffsets=adjustVehicleOffsetsAccordingToYellowTimes(actuationEventByDetectorList,
                yellowAndAllRedPropertyByID.yellowTimePropertyList);

        List<double[]> AllRedOffsets=adjustAllRedOffsetsAccordingToYellowTimes(yellowAndAllRedPropertyByID.redTimePropertyList,
                yellowAndAllRedPropertyByID.yellowTimePropertyList);

        XYSeriesCollection dataset = new XYSeriesCollection();
        XYSeries YellowEndTimes=new XYSeries(AllRedOffsets.size());
        XYSeries RedEndTimes=new XYSeries(AllRedOffsets.size());
        for(int i=0;i<AllRedOffsets.size();i++){
            YellowEndTimes.add(AllRedOffsets.get(i)[0]/3600,AllRedOffsets.get(i)[3]);
            RedEndTimes.add(AllRedOffsets.get(i)[0]/3600,AllRedOffsets.get(i)[2]);
        }
        YellowEndTimes.setKey("End Of Yellow");
        RedEndTimes.setKey("End Of All Red");
        XYSeries VehicleActuations=new XYSeries(VehicleOffsets.size());
        for(int i=0;i<VehicleOffsets.size();i++){
            VehicleActuations.add(VehicleOffsets.get(i)[0]/3600,VehicleOffsets.get(i)[2]);
        }
        VehicleActuations.setKey("Detector Actuation");
        dataset.addSeries(YellowEndTimes);
        dataset.addSeries(RedEndTimes);
        dataset.addSeries(VehicleActuations);

        XYPlot plot = new XYPlot();
        plot.setDataset(0, dataset);
        plot.setRenderer(0, new XYSplineRenderer());//use default fill paint for first series
        NumberAxis numberAxis=new NumberAxis("Yellow and Red Times (Sec)");
        numberAxis.setRange(0, 15);
        plot.setRangeAxis(0, numberAxis);
        NumberAxis numberAxis1=new NumberAxis("Hour of Day (Hr)");
        numberAxis1.setTickUnit(new NumberTickUnit(1));
        numberAxis1.setRange(0,24);
        plot.setDomainAxis(numberAxis1);
        plot.mapDatasetToRangeAxis(0, 0);

        XYLineAndShapeRenderer rendererShape = new XYLineAndShapeRenderer();
        rendererShape.setSeriesShapesVisible(0, false);
        rendererShape.setSeriesShapesVisible(1, false);
        rendererShape.setSeriesLinesVisible(2, false);
        rendererShape.setSeriesPaint(0,Color.yellow);
        rendererShape.setSeriesPaint(1,Color.red);
        rendererShape.setSeriesPaint(2,Color.black);
        rendererShape.setSeriesStroke(0, new BasicStroke(4));
        rendererShape.setSeriesStroke(1, new BasicStroke(4));
        double size=3;
        rendererShape.setSeriesShape(2,new Ellipse2D.Double(-size/2, -size/2, size, size));
        plot.setRenderer(0, rendererShape);

        //generate the chart
        JFreeChart chart = new JFreeChart(Title, plot);
        chart.setBackgroundPaint(Color.WHITE);
        final ChartPanel chartPanel = new ChartPanel(chart);
        //setContentPane(chartPanel);
        chartPanel.setPreferredSize(new java.awt.Dimension(1000, 500));
        final ApplicationFrame frame = new ApplicationFrame(Title);
        frame.setContentPane(chartPanel);
        frame.pack();
        frame.setVisible(true);
    }


    // Purdue Split Failure
    public static void drawPurdueSplitFailure(List<Integer> DetectorList,int PhaseID, String Movement, int Date
            , double FromTime, double ToTime,String IntIPStr, Connection con, String Title, double Interval){
        // This function is used to draw Purdue Split Failure

        Title= Title +" : Phase "+PhaseID + " & "+Movement;

        // Get the properties of current Phase ID
        PhasePropertyByID phasePropertyByID=getActualPhaseInfForGivenPhaseDateAndTimePeriod(Date,FromTime,ToTime,PhaseID,IntIPStr,con);
        List<double[]> GreenAndRedRefTimeByCycle=new ArrayList<double[]>(); // double[]{StartOfGreen,EndOfAllRed}
        List<PhaseProperty> phasePropertyList=phasePropertyByID.phasePropertyList;
        double DefaultYellowInterval=2;
        double DefaultAllRed=1;
        for(int i=0;i<phasePropertyList.size();i++){
            if(phasePropertyList.get(i).GreenStartTime>0 && phasePropertyList.get(i).GreenEndTime>0){
                // Valid green time
                double StartOfGreen=phasePropertyList.get(i).GreenStartTime;
                double EndOfAllRed=phasePropertyList.get(i).GreenEndTime;
                // Updated with Yellow duration
                if(phasePropertyList.get(i).YellowStartTime>0 && phasePropertyList.get(i).YellowEndTime>0){
                    EndOfAllRed=EndOfAllRed+phasePropertyList.get(i).YellowDuration;
                }else{
                    EndOfAllRed=EndOfAllRed+DefaultYellowInterval;
                }
                // Updated with All-Red duration
                if(phasePropertyList.get(i).AllRedStartTime>0 && phasePropertyList.get(i).AllRedEndTime>0){
                    EndOfAllRed=EndOfAllRed+phasePropertyList.get(i).AllRedDuration;
                }else{
                    EndOfAllRed=EndOfAllRed+DefaultAllRed;
                }
                GreenAndRedRefTimeByCycle.add(new double[]{StartOfGreen,EndOfAllRed});
            }
        }

        // Get the actuation events for the list of advance detectors belonging to the approach
        List<ActuationEventByDetector> actuationEventByDetectorList=getActuationEventsForGivenDateAndTimePeriodAndDetectorList(Date,FromTime,ToTime,
                IntIPStr, con,DetectorList);
        // double[]{CycleStartTime, GOR_Det1, ..., GOR_detn, GOR_Avg,FailureIndex}
        double[][] GreenOccRatio=new  double[GreenAndRedRefTimeByCycle.size()][actuationEventByDetectorList.size()+2];
        // double[]{CycleStartTime, ROR_Det1, ..., ROR_detn, ROR_Avg,FailureIndex}
        double[][] RedOccRatio=new  double[GreenAndRedRefTimeByCycle.size()][actuationEventByDetectorList.size()+2];
        // Index of split failure
        boolean[] SplitFailureList =new boolean[GreenAndRedRefTimeByCycle.size()];
        double BeginningOfRedDuration=5;// seconds after the activation of red
        double SplitFailureThreshold=80; // Threshold of 80%
        for(int i=0;i<GreenAndRedRefTimeByCycle.size();i++) {
            double StartTime=GreenAndRedRefTimeByCycle.get(i)[0];
            double EndTimeGreen=GreenAndRedRefTimeByCycle.get(i)[1];
            double EndTimeRed=EndTimeGreen+BeginningOfRedDuration;

            GreenOccRatio[i][0]=StartTime/3600.0;
            RedOccRatio[i][0]=StartTime/3600.0;
            SplitFailureList[i]=false;

            double AveGreenOccRatio=0;
            double AveRedOccRatio=0;
            for (int j = 0; j < actuationEventByDetectorList.size(); j++) {
                GreenOccRatio[i][j+1]=0;
                RedOccRatio[i][j+1]=0;
                List<ActuationEvent> actuationEventList=actuationEventByDetectorList.get(j).actuationEventList;
                for(int k=0;k<actuationEventList.size();k++){
                    // Within the green period
                    if(actuationEventList.get(k).OnTime>=StartTime && actuationEventList.get(k).OffTime<EndTimeGreen){
                        GreenOccRatio[i][j+1]=GreenOccRatio[i][j+1]+actuationEventList.get(k).OnDuration;
                    }else if(actuationEventList.get(k).OnTime>=StartTime && actuationEventList.get(k).OffTime>=EndTimeGreen){
                        GreenOccRatio[i][j+1]=GreenOccRatio[i][j+1]+(EndTimeGreen-Math.min(EndTimeGreen,actuationEventList.get(k).OnTime));
                    }else if(actuationEventList.get(k).OnTime<StartTime && actuationEventList.get(k).OffTime<EndTimeGreen){
                        GreenOccRatio[i][j+1]=GreenOccRatio[i][j+1]+(Math.max(StartTime,actuationEventList.get(k).OffTime)-StartTime);
                    }else if(actuationEventList.get(k).OnTime<StartTime && actuationEventList.get(k).OffTime>=EndTimeGreen){
                        GreenOccRatio[i][j+1]=GreenOccRatio[i][j+1]+(EndTimeGreen-StartTime);
                    }

                    // Within the red period
                    if(actuationEventList.get(k).OnTime>=EndTimeGreen && actuationEventList.get(k).OffTime<EndTimeRed){
                        RedOccRatio[i][j+1]=RedOccRatio[i][j+1]+actuationEventList.get(k).OnDuration;
                    }else if(actuationEventList.get(k).OnTime>=EndTimeGreen && actuationEventList.get(k).OffTime>=EndTimeRed){
                        RedOccRatio[i][j+1]=RedOccRatio[i][j+1]+(EndTimeRed-Math.min(EndTimeRed,actuationEventList.get(k).OnTime));
                    }else if(actuationEventList.get(k).OnTime<EndTimeGreen && actuationEventList.get(k).OffTime<EndTimeRed){
                        RedOccRatio[i][j+1]=RedOccRatio[i][j+1]+(Math.max(EndTimeGreen,actuationEventList.get(k).OffTime)-EndTimeGreen);
                    }else if(actuationEventList.get(k).OnTime<EndTimeGreen && actuationEventList.get(k).OffTime>=EndTimeRed){
                        RedOccRatio[i][j+1]=RedOccRatio[i][j+1]+(EndTimeRed-EndTimeGreen);
                    }
                }
                GreenOccRatio[i][j+1]=GreenOccRatio[i][j+1]/(EndTimeGreen-StartTime)*100;
                RedOccRatio[i][j+1]=RedOccRatio[i][j+1]/(EndTimeRed-EndTimeGreen)*100;

                AveGreenOccRatio=AveGreenOccRatio+GreenOccRatio[i][j+1]/actuationEventByDetectorList.size();
                AveRedOccRatio=AveRedOccRatio+RedOccRatio[i][j+1]/actuationEventByDetectorList.size();
            }
            GreenOccRatio[i][actuationEventByDetectorList.size()+1]=AveGreenOccRatio;
            RedOccRatio[i][actuationEventByDetectorList.size()+1]=AveRedOccRatio;

            if(AveGreenOccRatio>=SplitFailureThreshold&&AveRedOccRatio>=SplitFailureThreshold){
                SplitFailureList[i]=true;
            }
        }

        XYSeriesCollection dataset = new XYSeriesCollection();
        XYSeries GOR=new XYSeries(GreenOccRatio.length);
        XYSeries ROR=new XYSeries(RedOccRatio.length);
        XYSeries Threshold=new XYSeries(RedOccRatio.length);
        XYSeries SplitFailureSeries=new XYSeries(RedOccRatio.length);
        for(int i=0;i<GreenOccRatio.length;i++){
            //GOR.add(GreenOccRatio[i][0],GreenOccRatio[i][actuationEventByDetectorList.size()+1]);
            //ROR.add(RedOccRatio[i][0],RedOccRatio[i][actuationEventByDetectorList.size()+1]);
            GOR.add(GreenOccRatio[i][0],GreenOccRatio[i][2]);
            ROR.add(RedOccRatio[i][0],RedOccRatio[i][2]);
            Threshold.add(RedOccRatio[i][0],SplitFailureThreshold);
            if(SplitFailureList[i]){
                SplitFailureSeries.add(RedOccRatio[i][0],110);
            }
        }
        GOR.setKey("Green Occupancy Ratio");
        ROR.setKey("Red Occupancy Ratio");
        Threshold.setKey("Split Failure Threshold");
        SplitFailureSeries.setKey("Split Failure");
        dataset.addSeries(GOR);
        dataset.addSeries(ROR);
        dataset.addSeries(Threshold);
        dataset.addSeries(SplitFailureSeries);

        XYPlot plot = new XYPlot();
        plot.setDataset(0, dataset);
        plot.setRenderer(0, new XYSplineRenderer());//use default fill paint for first series
        NumberAxis numberAxis=new NumberAxis("Occupancy Ratio (Percent)");
        numberAxis.setRange(0, 120);
        plot.setRangeAxis(0, numberAxis);
        NumberAxis numberAxis1=new NumberAxis("Hour of Day (Hr)");
        numberAxis1.setTickUnit(new NumberTickUnit(1));
        numberAxis1.setRange(0,24);
        plot.setDomainAxis(numberAxis1);
        plot.mapDatasetToRangeAxis(0, 0);

        XYLineAndShapeRenderer rendererShape = new XYLineAndShapeRenderer();
        rendererShape.setSeriesShapesVisible(0, false);
        rendererShape.setSeriesShapesVisible(1, false);
        rendererShape.setSeriesShapesVisible(2, false);
        rendererShape.setSeriesLinesVisible(3, false);
        rendererShape.setSeriesPaint(0,Color.green);
        rendererShape.setSeriesPaint(1,Color.red);
        rendererShape.setSeriesPaint(2,Color.blue);
        rendererShape.setSeriesPaint(3,Color.red);
        rendererShape.setSeriesStroke(0, new BasicStroke(1));
        rendererShape.setSeriesStroke(1, new BasicStroke(1));
        rendererShape.setSeriesStroke(2, new BasicStroke(2));
        double size=3;
        rendererShape.setSeriesShape(3,new Ellipse2D.Double(-size/2, -size/2, size, size));
        plot.setRenderer(0, rendererShape);

        //generate the chart
        JFreeChart chart = new JFreeChart(Title, plot);
        chart.setBackgroundPaint(Color.WHITE);
        final ChartPanel chartPanel = new ChartPanel(chart);
        //setContentPane(chartPanel);
        chartPanel.setPreferredSize(new java.awt.Dimension(1000, 500));
        final ApplicationFrame frame = new ApplicationFrame(Title);
        frame.setContentPane(chartPanel);
        frame.pack();
        frame.setVisible(true);
    }

    // Arrival on red
    public static void drawArrivalOnRedByApproachAndPhase(DetectorByApproach detectorByApproach,int Date, double FromTime, double ToTime,String IntIPStr
            , Connection con, String Title, double Interval){
        // This function is used to draw arrivals on red with given approach and phase

        double SpeedLimit=detectorByApproach.SpeedLimit;
        int PhaseID=detectorByApproach.PhaseForThroughMovement;
        Title= Title +" : Phase "+PhaseID;
        List<Integer> DetectorList=new ArrayList<Integer>();
        double MaxLatency=0;// Projected time from advance detector to stopbar, in seconds
        for(int i=0;i<detectorByApproach.detectorConfigList.size();i++){
            if(detectorByApproach.detectorConfigList.get(i).Type.equals("AdvanceDetector") &&
                    detectorByApproach.detectorConfigList.get(i).DistanceFromStopbar>0){
                double Latency=(detectorByApproach.detectorConfigList.get(i).DistanceFromStopbar/5280)/SpeedLimit*3600;
                if(Latency>MaxLatency){
                    MaxLatency=Latency;
                }
                DetectorList.add(detectorByApproach.detectorConfigList.get(i).DetectorID);
            }
        }
        List<PhaseOnOffEvents> phaseOnOffEventsList=getPhaseOnOffEventsForGivenDateAndTimePeriod(PhaseID,Date,FromTime,ToTime,IntIPStr,con);
        List<ActuationEventByDetector> actuationEventByDetectorList=getActuationEventsForGivenDateAndTimePeriodAndDetectorList(Date,FromTime,ToTime,
                IntIPStr, con,DetectorList);

        double [][] arrivalOnRedFlow=calulateArrivalOnRedFlowForGivenPhaseAndTimePeriod(phaseOnOffEventsList, actuationEventByDetectorList
                , FromTime, ToTime, Interval, MaxLatency);
        drawArrivalOnRedWithDataInput(arrivalOnRedFlow,Title);
    }

    public static void drawArrivalOnRedWithDataInput(double [][] arrivalOnRedFlow, String Title){
        // This function is draw arrivals on red with data inputs

        XYSeriesCollection dataset1 = new XYSeriesCollection();
        XYSeriesCollection dataset2 = new XYSeriesCollection();

        XYSeries xySeriesActualFlow=new XYSeries(arrivalOnRedFlow.length);
        XYSeries xySeriesOnRedFlow=new XYSeries(arrivalOnRedFlow.length);
        XYSeries xySeriesPercent=new XYSeries(arrivalOnRedFlow.length);
        for(int i=0;i<arrivalOnRedFlow.length;i++){
            xySeriesOnRedFlow.add(arrivalOnRedFlow[i][0]/3600,arrivalOnRedFlow[i][2]);
            xySeriesActualFlow.add(arrivalOnRedFlow[i][0]/3600,arrivalOnRedFlow[i][3]);
            xySeriesPercent.add(arrivalOnRedFlow[i][0]/3600,arrivalOnRedFlow[i][4]);
        }
        xySeriesOnRedFlow.setKey("Arrival On Red");
        xySeriesActualFlow.setKey("Total Vehicles");
        xySeriesPercent.setKey("Percent Arrival On Red");

        dataset1.addSeries(xySeriesOnRedFlow);
        dataset1.addSeries(xySeriesActualFlow);
        dataset2.addSeries(xySeriesPercent);

        new XYYLineChartSeriesCollection(Title,"Hour of Day (hr)", "Hourly Flow (vph)","Percent (%)", dataset1, dataset2);
    }

    // Detector events & Approach flow and occupancy
    public static void drawFlowOccTimeByApproach(double [][] TimeFlowOcc, String Case, String Title){
        // This function is used to draw flow/Occ-time plots by approach

        // Get flow-time & occ-time series
        XYSeries xySeriesFlow=new XYSeries(TimeFlowOcc.length);
        XYSeries xySeriesOcc=new XYSeries(TimeFlowOcc.length);
        XYSeries xySeriesFlowOcc=new XYSeries(TimeFlowOcc.length);
        for(int i=0;i<TimeFlowOcc.length;i++){
            xySeriesFlow.add((TimeFlowOcc[i][0]/3600.0),TimeFlowOcc[i][1]);
            xySeriesOcc.add((TimeFlowOcc[i][0]/3600.0),TimeFlowOcc[i][2]);
            xySeriesFlowOcc.add(TimeFlowOcc[i][2],TimeFlowOcc[i][1]);
        }
        xySeriesFlow.setKey("Flow");
        xySeriesOcc.setKey("Occupancy");
        xySeriesFlowOcc.setKey("Flow-Occupancy");

        if(Case.equals("Flow")) {
            new XYLineChart(Title, "Time (Hour of Day)",
                    "Flow Counts (VPH)", xySeriesFlow);
        }
        else if(Case.equals("Occupancy")){
            new XYLineChart(Title, "Time (Hour of Day)",
                    "Average Occupancy(%)", xySeriesOcc);
        }else if(Case.equals("Time&Flow&Occupancy")){
            new XYYLineChart(Title, "Time (Hour of Day)",
                    "Flow Counts (VPH)","Average Occupancy(%)",xySeriesFlow, xySeriesOcc);
        }
        else if(Case.equals("Flow&Occupancy")){
            new XYScatterPlot(Title, "Average Occupancy(%)",
                    "Flow Counts (VPH)", xySeriesFlowOcc);
        }
    }

    public static void drawApproachFlowAndOccupancy(List<DetectorConfig> detectorConfigList, double Interval, String Title
            ,List<ActuationEventByDetector> actuationEventByDetectorList){
        // This function is used to draw approach flow and occupancy

        // Get the list of advance detectors
        List<IntersectionConfig.DetectorConfig> advanceDetectorList=IntersectionConfig.getListOfDetectorsByGivenType
                (detectorConfigList,"AdvanceDetector","NA");

        if(advanceDetectorList.size()>0) {
            List<Double> NumOfLanes = new ArrayList<Double>();
            double TotDetectorLanes=0;
            List<ActuationEventByDetector> selectedActualDetectorEvents = new ArrayList<ActuationEventByDetector>();
            for (int j = 0; j < advanceDetectorList.size(); j++) {
                for (int k = 0; k < actuationEventByDetectorList.size(); k++) {
                    if (actuationEventByDetectorList.get(k).DetectorID == advanceDetectorList.get(j).DetectorID) {
                        selectedActualDetectorEvents.add(actuationEventByDetectorList.get(k));
                        NumOfLanes.add((double) advanceDetectorList.get(j).NumOfLanes);
                        TotDetectorLanes=TotDetectorLanes+(double) advanceDetectorList.get(j).NumOfLanes;
                        break;
                    }
                }
            }
            List<FlowOccByDetector> flowOccByDetectorList=calculateTrafficSignalParameters.
                    getFlowOccForGivenDateAndTimePeriodAndInterval(selectedActualDetectorEvents,Interval,NumOfLanes);
            double [][] TimeFlowOcc= new double[flowOccByDetectorList.get(0).TimeFlowOcc.length][3];
            for(int j=0;j<flowOccByDetectorList.get(0).TimeFlowOcc.length;j++){
                TimeFlowOcc[j][0]=flowOccByDetectorList.get(0).TimeFlowOcc[j][0]; // Get time
                for(int k=0;k<flowOccByDetectorList.size();k++){
                    // Get the sum of flow
                    TimeFlowOcc[j][1]=TimeFlowOcc[j][1]+flowOccByDetectorList.get(k).TimeFlowOcc[j][1];
                    // Get the mean of occupancy
                    TimeFlowOcc[j][2]=TimeFlowOcc[j][2]+(flowOccByDetectorList.get(k).TimeFlowOcc[j][2]*NumOfLanes.get(k))/TotDetectorLanes;
                }
            }
            drawFlowOccTimeByApproach(TimeFlowOcc, "Time&Flow&Occupancy", Title);
            drawFlowOccTimeByApproach(TimeFlowOcc, "Flow&Occupancy", Title);
        }
    }

    public static void drawTurningMovementCountByApproach(List<DetectorConfig> detectorConfigList, double Interval, String Title
            ,List<ActuationEventByDetector> actuationEventByDetectorList, String Movement){
        // This function is used to draw turning movement counts by approach: only for left-turn and right-turn movements

        // Get the list of left-turn detectors
        List<IntersectionConfig.DetectorConfig> leftTurnDetectorList=IntersectionConfig.getListOfDetectorsByGivenType
                (detectorConfigList,"VehicleDetector",Movement);

        if(leftTurnDetectorList.size()>0) { // If exclusive left turn detectors exist
            List<Double> NumOfLanes = new ArrayList<Double>();
            double TotDetectorLanes=0;
            List<ActuationEventByDetector> selectedActualDetectorEvents = new ArrayList<ActuationEventByDetector>();
            for (int j = 0; j < leftTurnDetectorList.size(); j++) {
                for (int k = 0; k < actuationEventByDetectorList.size(); k++) {
                    if (actuationEventByDetectorList.get(k).DetectorID == leftTurnDetectorList.get(j).DetectorID) {
                        selectedActualDetectorEvents.add(actuationEventByDetectorList.get(k));
                        NumOfLanes.add((double) leftTurnDetectorList.get(j).NumOfLanes);
                        TotDetectorLanes=TotDetectorLanes+(double) leftTurnDetectorList.get(j).NumOfLanes;
                        break;
                    }
                }
            }
            List<FlowOccByDetector> flowOccByDetectorList=calculateTrafficSignalParameters.
                    getFlowOccForGivenDateAndTimePeriodAndInterval(selectedActualDetectorEvents,Interval,NumOfLanes);
            double [][] TimeFlowOcc= new double[flowOccByDetectorList.get(0).TimeFlowOcc.length][3];
            for(int j=0;j<flowOccByDetectorList.get(0).TimeFlowOcc.length;j++){
                TimeFlowOcc[j][0]=flowOccByDetectorList.get(0).TimeFlowOcc[j][0]; // Get time
                for(int k=0;k<flowOccByDetectorList.size();k++){
                    // Get the sum of flow
                    TimeFlowOcc[j][1]=TimeFlowOcc[j][1]+(flowOccByDetectorList.get(k).TimeFlowOcc[j][1]*NumOfLanes.get(k))/TotDetectorLanes;
                    // Get the mean of occupancy
                    TimeFlowOcc[j][2]=TimeFlowOcc[j][2]+(flowOccByDetectorList.get(k).TimeFlowOcc[j][2]*NumOfLanes.get(k))/TotDetectorLanes;
                }
            }
            drawFlowOccTimeByApproach(TimeFlowOcc, "Time&Flow&Occupancy", Title);
            drawFlowOccTimeByApproach(TimeFlowOcc, "Flow&Occupancy", Title);
            drawFlowOccTimeByApproach(TimeFlowOcc, "Flow", Title);
        }
    }

    // Pedestrian details
    public static void drawPedestrianCountsAndDelayByPhase(int PhaseID,int Date, double FromTime, double ToTime,String IntIPStr
            , Connection con, String Title, double Interval){
        // This function is used to draw pedestrian counts and delay by phases

        PedestrianCallByPhase pedestrianCallByPhase=getPedestrianCallByPhase(PhaseID,Date, FromTime, ToTime,IntIPStr, con);
        List<WaitingTimeForPedestrianCall> waitingTimeForPedestrianCallList=calculateWaitingTimeForPedestrianCalls(pedestrianCallByPhase);

        TotalPedCallAndAvgWaitingTimeByInterval totalPedCallAndAvgWaitingTimeByInterval=getTotalPedCallAndAvgWaitingTimeByInterval
                (waitingTimeForPedestrianCallList,FromTime,ToTime, Interval);

        drawPedestrianEventsHistogram(totalPedCallAndAvgWaitingTimeByInterval.StartTime,totalPedCallAndAvgWaitingTimeByInterval.AvgWaitingTime
                , "Average Waiting Time(sec)",Title,"Time of Day (Hr)", "Waiting Time (Sec)", "Pedestrian Events");

        drawPedestrianEventsHistogram(totalPedCallAndAvgWaitingTimeByInterval.StartTime,totalPedCallAndAvgWaitingTimeByInterval.MaxWaitingTime
                , "Maximum Waiting Time(sec)",Title,"Time of Day (Hr)", "Waiting Time (Sec)", "Pedestrian Events");

        drawPedestrianEventsHistogram(totalPedCallAndAvgWaitingTimeByInterval.StartTime,totalPedCallAndAvgWaitingTimeByInterval.TotalPedCalls
                , "Total Pedestrian Calls",Title,"Time of Day (Hr)", "Number of Calls(#)", "Pedestrian Events");

    }

    public static void drawPedestrianEventsHistogram(double[] StartTime,double[] YData, String Key,String Title
            , String XLabel, String YLabel, String ApplicationTitle){

        XYSeries xySeriesAvg=new XYSeries(StartTime.length);
        for(int i=0;i<StartTime.length;i++){
            xySeriesAvg.add(StartTime[i]/3600.0,YData[i]);
        }
        xySeriesAvg.setKey(Key);
        final XYSeriesCollection dataset = new XYSeriesCollection(xySeriesAvg);
        final JFreeChart chart = ChartFactory.createXYBarChart(Title,XLabel,false,YLabel,dataset,
                PlotOrientation.VERTICAL, true, false,false
        );
        XYPlot xyPlot = (XYPlot)chart.getPlot();
        XYBarRenderer xyrend = (XYBarRenderer) xyPlot.getRenderer();
        xyrend.setMargin(0.9);

        ChartFactory.setChartTheme(StandardChartTheme.createJFreeTheme());
        final ChartPanel chartPanel = new ChartPanel(chart);
        chartPanel.setPreferredSize(new java.awt.Dimension(1000, 500));
        final ApplicationFrame frame = new ApplicationFrame(ApplicationTitle);
        frame.setContentPane(chartPanel);
        frame.pack();
        frame.setVisible(true);
    }

}
