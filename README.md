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
  - For a third visualization, we welcome your suggestions, be creative and tell us what you’d like to see ❤️

#### Technical Details and workflow:
