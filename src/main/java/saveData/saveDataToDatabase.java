package saveData;

import loadData.*;
import java.sql.*;
import java.util.*;

public class saveDataToDatabase{

    /**
     *
     * @param connection database connection
     * @param eventDataByIntersections  java class
     */
    public static void insertSiemensDataToDataBase(Connection connection,loadSiemensEventData.EventDataByIntersection eventDataByIntersections){
        // This function is used to insert Siemens data to database

        try {
            String IntersectionIP=eventDataByIntersections.getIntersectionIP();
            String Header="insert into siemens_database."+IntersectionIP+"_";
            List<loadSiemensEventData.EventData> eventDataList=eventDataByIntersections.getEventDataList();

            // First check the number of years inside the data
            HashSet<String> YearHashSet= new HashSet<String>();; // Create the hash set
            for(int i=0;i<eventDataList.size();i++){
                YearHashSet.add(Integer.toString(eventDataList.get(i).getEventDate()/10000));
            }
            if(YearHashSet.size()==0){
                System.out.println("No specific year in the data file!");
                System.exit(-1);
            }

            Iterator iterator = YearHashSet.iterator();
            while(iterator.hasNext()){
                int Year=Integer.parseInt(iterator.next().toString());
                String sql;
                String arrayElement;
                List<String> stringSiemens= new ArrayList<String>();
                Statement ps = connection.createStatement();
                HashSet<String> stringHashSet= new HashSet<String>();; // Create the hash set
                for (int i=0;i<eventDataList.size();i++) // Loop for each row
                {
                    arrayElement=eventDataList.get(i).getEventDate() + "-" + eventDataList.get(i).getEventTime() + "-" +
                            eventDataList.get(i).getEventID();
                    sql =  Header+ Year + " values (\"" +
                            eventDataList.get(i).getEventDate() + "\",\"" + eventDataList.get(i).getEventTime() + "\",\"" +
                            eventDataList.get(i).getEventTimeStr() + "\",\"" +
                            eventDataList.get(i).getEventID() + "\",\"" + eventDataList.get(i).getEventValue()+ "\");";
                    if(stringHashSet.add(arrayElement))// Able to add to the hash set
                    {// Copy the unique item
                        stringSiemens.add(sql);
                    }
                }
                // Insert the strings to database
                insertSQLBatch(ps, stringSiemens,1000);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

    }

    /**
     *
     * @param ps SQL Statement
     * @param string List of SQL strings
     * @param definedSize predefined size of lines to write into the database
     * @return true/false
     */
    public static boolean insertSQLBatch(Statement ps, List<String> string, int definedSize){
        // This function is used to insert SQL batch
        int curSize=0;
        List<String> tmpString= new ArrayList<String>();
        try {
            for (int i = 0; i < string.size(); i++) {
                ps.addBatch(string.get(i));
                tmpString.add(string.get(i));
                curSize=curSize+1;
                if(curSize==definedSize || i==string.size()-1){
                    try {
                        ps.executeBatch();
                    }catch (SQLException e){
                        ps.clearBatch();
                        insertLineByLine(ps, tmpString);
                    }
                    curSize=0;
                    tmpString=new ArrayList<String>();
                    ps.clearBatch();
                }
            }
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     *
     * @param ps SQL Statement
     * @param string List of SQL strings
     * @return true/false
     */
    public static boolean insertLineByLine(Statement ps, List<String> string){
        // This function is used to insert line by line

        for (int i=0;i<string.size();i++){
            try{
                ps.execute(string.get(i));
            } catch (SQLException e) {
                System.out.println("Fail to insert: "+e.getMessage());
            }
        }
        return true;
    }
}
