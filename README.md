# ATSPMS-Siemens
This is the package that uses Siemens' high resoluition data to compute Automated Traffic Signal Performance Measures (ATSPMs). 

## Available performance metrics:
- Split Monitor
- Purdue Phase Termination
- Purdue Coordination Diagram
- Purdue Split Failure
- Yellow and Red Actuation
- Arrival On Red
- Turning Movement Counts
- Approach Flow and Occupancy
- Pedestrian Activities

## Study site and data
- Four intersections along 2nd Ave in Seattle WA are used: 
(1) 2nd Ave & Broad St
(2) 2nd Ave & Battery St
(3) 2nd Ave & Pike St
(4) 2nd Ave & Spring St
- High-resolution data is available from Feb 28th, 2018 to March 7th, 2018.

## Before you run
- You need to load the data dump "Siemens_Data_Dump20180720.sql" into a MySQL database. 
- You also need to change the settings of "hostSiemens", "userName", and "password" correspondingly 
in src\main\java\main\MainSiemensATSPMS.java. 


## When you run
- You can: (i) change date and time, (ii) select one of the four intersections, and (iii) specify
the folders to load ATSPMs files in src\main\java\main\MainSiemensATSPMS.java.
- You can select to run/display one of the nine aformentioned performance metrics.
- Currently, the configuration file, including road geometry, detector layout, and signal settings are hard-coded
 in \ATSPMs\IntersectionConfig.java.
- The core code is provided in \ATSPMs\calculateTrafficSignalParameters.java.