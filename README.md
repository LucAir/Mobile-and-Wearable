# Mobile-and-Wearable

### TITLE: WORKFLOW GUARDIAN  
Have you ever wondered what it feels like to have your work or study sessions protected by a guardian? No? Well, it’s time to find out 😊.

#### How does it work
- Suppose you want to study but are bored of the classic apps that show only a timer and a small customizable character. You decide to try something new, and that’s how you find **WorkflowGuardian**.  
- When you open the app, registration is required, but don’t worry, it’s very quick!  
- Once you log in for the first time, a small test is required. Just stay relaxed for 2 minutes, and we will record your heartbeat.  
- Why do we need it?  
  - Heartbeat recording is used to calculate metrics such as **Heart Rate** and **Heart Rate Variability**. This information is stored in our database and used for our stress detection model. This ensures that our “fake machine learning model” (as decided in class, using just an if-else) is as accurate as possible.  
- After this test, you are ready to explore the app. On the home page, you will see a timer set to zero.  
- Click the timer to start a study session. On this page, to avoid distractions, you can also disable notifications.

#### Features
You might be wondering what makes our app different from others on the market:  
- We have a customizable character: a real guardian. Once you create an account, we gift you 500 tokens. Every minute you stay focused, a token will be added to your account. You can use these tokens to buy skins for your guardian, change its background, and even assign a pet to it (so it doesn’t feel lonely 🥺).  
- If you’re a bit of a nerd 🤓 and love charts, we’ve got you covered. In the **Analytics 📊** section, you’ll find two types of visualizations showing how your sessions are going:  
  - **Line Chart**: Shows details about your last session. Green areas indicate focused periods, while red and yellow points indicate lost attention (red) or moments when focus was dropping but you were still able to study (yellow).  
  - **Boxplot**: Allows you to compare different sessions. By clicking on a section, you can see the number of distractions. The Y-axis shows your heartbeat, so you can see in which sections you were more agitated.  
  - For a third visualization, we welcome your suggestions, be creative and tell us what you’d like to see. ❤️

### Technical Details and workflow

#### How Session will be recorder
Since our application does not connct to a real device (smarthwatch) we created 2 realistic session. This session are under res/raw/two_session.csv. have been created using a python script. 
The file is structured with two column (session ID, timestamp of each heartbeat).
Each session contains 3600 points. 
We read this session and we compute RR, and HRV, then we compute bpm and HRV to produce 60 values (more usable than 3600). The goal here is to show that a device will handle heartbeat as timestamp, and we are able to produce from there bpm.
So realistically if you want to simulate focus mode, what happens is that doesn't matter when you stop the timer, the recorded session will always be of one hour. This was done just for let the user see a real visualization and provide (in the boxplot chart) a comparison between two session.
In each chart also user can see some detail, not to technical of course, but just to provide a more clear idea of whats happening.

#### Machine Learning (Fake) Algorithm
Here as I mentioned before in the "more user friendly explanation" we decided to register the user heartbeat for two minutes the first time it log-in the application. In this way we can compute the baseline heartbeat (as the average of the two min recording) and the baseline hrv (in the same way). From there the algorithm to detect stress simply add values to this baseline values. If a value recorded in the session is higher or lower than the baseline + / - a value, we get a warnings or a critical point.


