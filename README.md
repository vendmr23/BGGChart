# BGGChart
Java project to generate compound images of game collections on BoardGameGeek

Modify the run.bat file as needed

The parameters are:
 - BGG user name
 - Individual image size in pixels
 - Number of horizontal images
 - Number of vertical images
 
 If you have more images than can be supported by the vertical x horizontal, 
 then multiple images will be created.
 
 The BGG query fetches OWNED items in your collection and excludes expansions.
 
 Project requires Java 8, was built in IntelliJ IDEA and uses gradle as the build tool.
