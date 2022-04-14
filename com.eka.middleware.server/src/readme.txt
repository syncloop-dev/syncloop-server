Starting Eka middleware server:

Windows:-
double click on start.bat to start the server.

Configuration steps:-
1. Download your OS specific JDK11. JVM for windows already included in the ekamw.zip. If not then download JDK11 from "https://docs.microsoft.com/en-us/java/openjdk/download".
2. Configure command for your OS. Take an example from start.bat
3. Configure middleware home dir in the config file(<installation folder>\ekamw\resources\config\server.properties)
   middleware.server.home.dir=<\folder\location>/Middleware/
4. Run the command.

Windows Service:-
Run the <ekamw home>\service\installService.bat to install "Eka middleware" service in windows services.
logs will be available at <ekamw home>\service\logs\*
* if you place your ekamw installation on any other drive other than D: then change the configurtions mentioned on 3rd step and update the service configuration <ekamw home>\service\EkamwSW-x64.xml

user:admin
password:admin

Workspace: http://localhost:8080/files/gui/middleware/pub/server/ui/workspace/web/workspace.html
Public page: http://localhost:8080/files/gui/middleware/pub/server/ui/welcome/index.html