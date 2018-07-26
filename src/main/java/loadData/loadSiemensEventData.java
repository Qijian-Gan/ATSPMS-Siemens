package loadData;

import saveData.saveDataToDatabase;
import java.sql.Connection;
import java.util.ArrayList;
import java.io.*;
import java.util.*;

public class loadSiemensEventData {
    // This function is used to load Siemens Event based data

    public static class EventData{
        // This is the profile for Event Data
        public EventData(int _EventDate, double _EventTime,String _EventTimeStr, int _EventID,int _EventValue){
            this.EventDate=_EventDate;
            this.EventTime=_EventTime;
            this.EventTimeStr=_EventTimeStr;
            this.EventID=_EventID;
            this.EventValue=_EventValue;
        }
        protected int EventDate;
        protected double EventTime;
        protected String EventTimeStr;
        protected int EventID;
        protected int EventValue;

        public int getEventDate(){
            return EventDate;
        }

        public double getEventTime(){
            return EventTime;
        }

        public String getEventTimeStr(){
         return EventTimeStr;
        }

        public int getEventID(){
            return EventID;
        }

        public int getEventValue(){
            return EventValue;
        }
    }

    public static class EventDataByIntersection{
        // This is the profile for event data by intersection
        public EventDataByIntersection(String _IntersectionIP, List<EventData> _eventDataList){
            this.IntersectionIP=_IntersectionIP;
            this.eventDataList=_eventDataList;
        }
        protected String IntersectionIP;
        protected List<EventData> eventDataList;

        public String getIntersectionIP(){
            return IntersectionIP;
        }

        public List<EventData> getEventDataList(){
            return eventDataList;
        }
    }

    /**
     *
     * @param connection Database connection
     * @param InputFileLocation Input folder location
     * @param OutputFileLocation Output folder location (remove files to)
     */
    public static void mainSiemensRead(Connection connection,String InputFileLocation, String OutputFileLocation){
        // This the main function to read Siemens Data

        // Get the list of files
        File fileDir = new File(InputFileLocation);
        File[] listOfFiles = fileDir.listFiles();
        for (int i = 0; i < listOfFiles.length; i++) {// Loop for each file
            if (listOfFiles[i].isFile()) {
                // Reading the file
                String DataFileName = InputFileLocation+ "\\" + listOfFiles[i].getName();
                long startTime = System.currentTimeMillis();
                EventDataByIntersection eventDataByIntersection=readSiemensEventData(DataFileName);
                long endTime = System.currentTimeMillis();
                System.out.println("Process Time(ms):" + (endTime-startTime)+ " & Rows Processed:"+eventDataByIntersection.eventDataList.size());

                // Saving the data
                startTime = System.currentTimeMillis();
                saveDataToDatabase.insertSiemensDataToDataBase(connection,eventDataByIntersection);
                endTime = System.currentTimeMillis();
                System.out.println("Time for inserting data to database(ms):" + (endTime-startTime));
            }
        }
        // Remove the files
        for (int j=0;j<listOfFiles.length;j++) {
            moveFileFromAToB(InputFileLocation, OutputFileLocation,listOfFiles[j].getName());
        }
    }

    /**
     *
     * @param fileName File name
     * @return EventDataByIntersection (class)
     */
    public static EventDataByIntersection readSiemensEventData(String fileName){
        // This function is used to read the Siemens Data
        // Each file contains the event data for only one intersection

        File inputFile = new File(fileName);
        if(!inputFile.exists())
        {
            System.out.println("Can not find the event data file!");
            return null;
        }
        else{
            System.out.println("Loading:"+inputFile);
        }
        String line=null;
        try {
            List<EventData> eventDataList=new ArrayList<EventData>();

            FileReader fr= new FileReader(inputFile);
            BufferedReader br = new BufferedReader(fr);

            // The first line: the file header, which stores the intersection IP
            line = br.readLine();
            String[] tmp=line.split(",");
            String IntersectionIP=tmp[0].replace(".","_");

            while ((line = br.readLine()) != null && !line.equals("")) { // Loop for the following rows
                String[] tmpdataRow = line.split(",");
                List<String> dataRow = new ArrayList<String>();
                for (String tmpRow: tmpdataRow) {
                    if (!tmpRow.equals("")) {
                        dataRow.add(tmpRow);
                    }
                }
                // Get the date and time
                String [] DateTime=dataRow.get(0).split(" ");
                String[] DateStr=DateTime[0].split("-");
                int EventDate=Integer.parseInt(DateStr[2].trim())*10000+Integer.parseInt(DateStr[0].trim())*100+
                        Integer.parseInt(DateStr[1].trim());
                String EventTimeStr=DateTime[1].trim();
                String[] TimeStr=DateTime[1].split(":");
                double EventTime=Double.parseDouble(TimeStr[0].trim())*3600.0+Double.parseDouble(TimeStr[1].trim())*60.0+
                        Double.parseDouble(TimeStr[2].trim());
                // Get the event ID
                int EventID=Integer.parseInt(dataRow.get(1).trim());
                // Get the event value
                int EventValue=Integer.parseInt(dataRow.get(2).trim());

                EventData eventData=new EventData(EventDate,EventTime,EventTimeStr,EventID,EventValue);
                eventDataList.add(eventData);
            }
            br.close();
            fr.close();

            EventDataByIntersection eventDataByIntersection=new EventDataByIntersection(IntersectionIP,eventDataList);
            return eventDataByIntersection;
        }catch (IOException e)
        {
            e.printStackTrace();
            return null;
        }
    }

    /**
     *
     * @param fromFolder Current folder
     * @param toFolder Target folder
     * @param fileName file to be removed
     * @return true/false
     */
    public static boolean moveFileFromAToB(String fromFolder, String toFolder, String fileName){
        // This function is used to move file from fromFolder to toFolder

        File afileDir= new File(fromFolder);
        File afile = new File(afileDir,fileName);

        File bfileDir= new File(toFolder);
        File bfile = new File(bfileDir,fileName);

        if(afile.renameTo(bfile)){
            return true;
        }else{
            System.out.println("Fail to remove file:"+afile.getName());
            return false;
        }
    }

}
