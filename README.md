# Ros2BridgeMobile

## Description
Ros2BridgeMobile is an Android application that allows you to send messages from your mobile device to a ROS 2 client via rosbridge.  It supports publishing, subscribing, and managing custom messages, services, and actions directly from an android mobile device. The app supports standard, geometry, and custom protocol messages, controller/gamepad input, reusable message templates, and dynamic topic discovery.

Key features:
- Connect to ROS 2 networks using rosbridge protocol
- Publish to any topic with custom, standard, geometry, or protocol-based message types
- Save, edit, and reuse message templates and custom publisher buttons
- Subscribe to and view live ROS 2 topics with dynamic topic/type discovery (including image support)
- Import and manage custom .msg, .srv, and .action protocols with UI configuration
- Compose and send geometry_msgs, std_msgs, and custom messages interactively
- Manage and assign controller/gamepad inputs for remote robot control
- Service and action call support with queuing, status tracking, and result handling
- View and manage message history, custom publishers, and protocol actions
- Jetpack Compose tab for interactive message history and UI navigation
- Persistent storage for user preferences, custom actions, and publishers
- Modern UI with fragment switching, dropdowns, and configuration panels
- Export and import created profiles with app actions
- Custom user settable presets for yxba buttons cyclable forwards and backwards

Built with Kotlin, Jetpack Compose, and Android best practices. Networking uses OkHttp; message handling uses kotlinx.serialization and Gson.

## Table of Contents
- [Description](#description)
- [Installation](#installation)
- [Usage](#usage)
- [Credits](#credits)

## Installation
1. Clone this repository:
   ```sh
   git clone https://github.com/WinstonHar/Ros2BridgeMobile.git
   ```
2. Open the project in Android Studio.
3. Connect your Android device or start an emulator.
4. Build and run the app from Android Studio.

**Dependencies:**
- Android Studio (latest recommended)
- Android SDK 24+
- Internet connection to access your ROS 2 network
- ROS 2 setup with rosbridge_server running (see [rosbridge_suite](https://github.com/RobotWebTools/rosbridge_suite))

**Tested Equipment**
- Android 12 - Sony Xperia 1 II XQ-AT51 58.A.7.93
- Android 13 - Retroid Pocket 4 Pro TP1A.220624.014
- Ros2 - Noble, Jazzy
- Android Studio 2025.1.1
## Usage
1. Start your ROS 2 network and ensure `rosbridge_server` is running (default port 9090).
2. Open Ros2 Bridge on your Android device.
3. Enter the IP address and port of your rosbridge server.
4. Connect to the server.
5. Use the dropdown to select different message types or features:
   - Default publisher
   - Custom publisher (standard messages)
   - Geometry standard messages
   - Slider controls (create and use)
   - Controller input device
6. Fill in message fields and publish, or subscribe to topics to view live data.
7. Save reusable messages for quick access.
8. Use the message history tab to review sent messages.

Specific instructions for using controller input for remote robot movement.
1. Start Ros 2 rosbridge_server on robot env.
2. Connect to IP and port of robot env while connected to the same wifi network.
3. Once connection is esablished select Geometry Standard Message action.
4. Enter topic name your robot is listening to. Note: if you switch off to a different 'action' your inputs will be erased
5. Select message type (for instance twist)
6. Fill in all values.
7. Save values using grey 'save as reusable button' button. Note: publish button allows you to directly advertise and publish values before saving. When pressed the app will prompt you to enter a name.
8. Save all buttons you need. Then swap to Controller Input Device Action.
9. On the left you can see the Available App Actions showing all your saved buttons, check to make sure they are correct, if not you can go back to the Geometry Standard Messages page and delete and rewrite a fixed version. The arrow in the center colapses this pane.
10. Assign controller buttons to actions. Note: They layout for android controller support follows the xbox layout.
11. Below is stick controls. Input 1 is tracked topic. Input 2 is message type. Input 3 is max value. Input 4 is step value(to control sensitivity/accelleration). Input 5 is deadzone (min value). Then save.
12. From here when you leave this action open the controller inputs (when connected to the bluetooth of the phone) will be mapped to the inputs you assigned. You can also access these controls via the controller overview ui button at the bottom of the screen.

**Screenshots:**
<div align="center">
  <img src="screenshots\Screenshot_20250724-115018.png" alt="Main UI" width="400"/>
  <br/><em>Main UI</em>
</div>
<div align="center">
  <img src="screenshots\Screenshot_20250724-115034.png" alt="Custom Msg" width="400"/>
  <br/><em>Standard messages app action setting pane</em>
</div>
<div align="center">
  <img src="screenshots\Screenshot_20250724-115048.png" alt="Geometry Msg" width="400"/>
  <br/><em>Geometry messages app action setting pane</em>
</div>
<div align="center">
  <img src="screenshots\Screenshot_20250724-115104.png" alt="Slider buttons" width="400"/>
  <br/><em>Slider buttons app action setting pane</em>
</div>
<div align="center">
  <img src="screenshots\Screenshot_20250724-115127.png" alt="Controller Input Device Pane" width="400"/>
  <br/><em>Controller input setting pane, app action and controller buttons</em>
</div>
<div align="center">
  <img src="screenshots\Screenshot_20250724-115150.png" alt="Controller preset and joysticks" width="400"/>
  <br/><em>Controller input pane, preset area and joysticks </em>
</div>
<div align="center">
  <img src="screenshots\Screenshot_20250724-115203.png" alt="Import export" width="400"/>
  <br/><em>Controller input pane, joysticks and import export configs</em>
</div>
<div align="center">
  <img src="screenshots\Screenshot_20250724-115223.png" alt="Custom protocols" width="400"/>
  <br/><em>Custom Protocols, root package and selector pane</em>
</div>
<div align="center">
  <img src="screenshots\Screenshot_20250724-115235.png" alt="Custom protocols configurator" width="400"/>
  <br/><em>Custom Protocols, configurator, buttons</em>
</div>
<div align="center">
  <img src="screenshots\Screenshot_20250724-115308.png" alt="Subscriber Activity" width="400"/>
  <br/><em>Subscriber Activity (not populated)</em>
</div>
<div align="center">
  <img src="screenshots\Screenshot_20250724-115321.png" alt="Overview UI" width="400"/>
  <br/><em>Overview UI</em>
</div>

**Custom Protocol Support**
Now pre compile you can add custom messages. These are located in the root > app > src > main > assets > msgs folder.
In the msgs folder the structure must be as below to be registed in the UI automatically.
Msgs >
 -- action
 -- msg
 -- srv

The "action" folder should contain .action files
The "msg" folder should contain .msg files
The "srv" folder should contain .srv files

After adding in files to the respective folders they will show in the UI in their respective categories in a checkbox list. From there users can select which ones they want to use in the application and set custom buttons with prefilled values to use on the controller. 
Right now I have filters for text casing to exclude hint values. So only snake case variables in the files will be fillable while variables in all caps will be locked. This can be used to show what values correspond to different functions for fillable variables without writing external docs.

NOTE: In order to get custom protocols working at least with the version of rosbridge I am using you need to manually add the imports to the __init__.py file for the actions into the incorrect folders in order for rosbridge to find them. 

Example: For the action RunPose I need to manually make these imports
- msgs/srv/__init__.py -> from msgs.action._run_pose import RunPose_SendGoal, RunPose_GetResult
- msgs/action/__init__.py -> from ._run_pose import RunPose_Feedback, RunPose_Result, RunPose_Goal, RunPose_SendGoal, RunPose_GetResult
- msgs/msg/__init__.py -> from msgs.action import RunPose_Feedback, RunPose_Result, RunPose_Goal

This has to be done for every action type you want to support. If you fail to do these imports rosbridge will say the funcitions for x action cannot be found and refuse to send said action.

**Horizontal UI Improvements**
Now accessible in the main activity UI at the bottom there is a controller overview UI! After setting up your connection to ros via ip/port selector and importing or creating your app actions you can now use your connected controller with a clean UI showing only simple large descriptions of what each button is set to. Additionally if you use the preset selector the center yxba buttons cycle and a pop up UI showing all available presets appears for 1.5 seconds. Additionally if you subscribe to an image feed it will replace the background of the ui allowing for image feed viewing while controlling your robot. YXBA buttons on the screen are also functional. Selecting subtext on the screen will show a detailed description of the app action.

## Credits
- Developed by WinstonHar
- Uses [rosbridge_suite](https://github.com/RobotWebTools/rosbridge_suite) for ROS 2 communication
- Built with Kotlin, Jetpack Compose, OkHttp, and AndroidX libraries
- Icon: "Arrow Top 18" by Catalin Fertu, from the Bigmug Interface Icons collection  
  Licensed under [CC Attribution License](https://creativecommons.org/licenses/by/4.0/)  
  Source: [SVG Repo](https://www.svgrepo.com/svg/429771/arrow-top-18)
