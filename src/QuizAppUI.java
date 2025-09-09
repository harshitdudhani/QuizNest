import java.awt.*;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.List;
import javax.swing.*;
import javax.swing.Timer;

public class QuizAppUI extends JFrame {
    // UI Components
    private JButton soloButton, apiButton, pvpButton;
    private JTextArea questionArea;
    private JButton[] optionButtons = new JButton[4];
    private JLabel timerLabel, scoreLabel, statusLabel;
    private Timer questionTimer;

    // Game State
    private List<Question> questions;
    private int currentQuestionIndex = 0;
    private int score = 0;
    private int timeLeft = 30;
    private String playerName;
    private String opponentName;
    private boolean isHost = false;

    // Networking
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;

    // Card Layout
    private CardLayout cardLayout;
    private JPanel cardPanel;
    private JPanel menuPanel, quizPanel, networkPanel;
    private JButton hostButton, joinButton;

    public QuizAppUI() {
        setTitle("Quiz Game");
        setSize(600, 500);
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        // Initialize Card Layout
        cardLayout = new CardLayout();
        cardPanel = new JPanel(cardLayout);

        // Create Panels
        createMenuPanel();
        createQuizPanel();
        createNetworkPanel();

        // Add panels to card layout
        cardPanel.add(menuPanel, "MENU");
        cardPanel.add(quizPanel, "QUIZ");
        cardPanel.add(networkPanel, "NETWORK");

        add(cardPanel);

        // Show menu initially
        showMenu();
    }

    private void createMenuPanel() {
        menuPanel = new JPanel();
        menuPanel.setLayout(new BoxLayout(menuPanel, BoxLayout.Y_AXIS));
        menuPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel titleLabel = new JLabel("Quiz Game", JLabel.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 24));
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        soloButton = new JButton("Solo Quiz");
        soloButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        soloButton.addActionListener(e -> startSoloQuiz());

        apiButton = new JButton("API Quiz");
        apiButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        apiButton.addActionListener(e -> startApiQuiz());

        pvpButton = new JButton("1v1 Quiz");
        pvpButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        pvpButton.addActionListener(e -> showNetworkOptions());

        menuPanel.add(Box.createVerticalStrut(20));
        menuPanel.add(titleLabel);
        menuPanel.add(Box.createVerticalStrut(30));
        menuPanel.add(soloButton);
        menuPanel.add(Box.createVerticalStrut(15));
        menuPanel.add(apiButton);
        menuPanel.add(Box.createVerticalStrut(15));
        menuPanel.add(pvpButton);
    }

    private void createQuizPanel() {
        quizPanel = new JPanel();
        quizPanel.setLayout(new BoxLayout(quizPanel, BoxLayout.Y_AXIS));
        quizPanel.setBorder(BorderFactory.createEmptyBorder(10, 20, 20, 20));

        statusLabel = new JLabel("", JLabel.CENTER);
        statusLabel.setFont(new Font("Arial", Font.PLAIN, 16));

        timerLabel = new JLabel("Time: 30s", JLabel.CENTER);
        timerLabel.setFont(new Font("Arial", Font.BOLD, 16));

        questionArea = new JTextArea();
        questionArea.setFont(new Font("Arial", Font.PLAIN, 18));
        questionArea.setLineWrap(true);
        questionArea.setWrapStyleWord(true);
        questionArea.setEditable(false);
        questionArea.setBackground(getBackground());

        JPanel optionsPanel = new JPanel(new GridLayout(2, 2, 10, 10));
        for (int i = 0; i < 4; i++) {
            optionButtons[i] = new JButton();
            optionButtons[i].setFont(new Font("Arial", Font.PLAIN, 14));
            final int index = i;
            optionButtons[i].addActionListener(e -> handleAnswer(index));
            optionsPanel.add(optionButtons[i]);
        }

        scoreLabel = new JLabel("Score: 0", JLabel.CENTER);
        scoreLabel.setFont(new Font("Arial", Font.BOLD, 18));

        quizPanel.add(statusLabel);
        quizPanel.add(timerLabel);
        quizPanel.add(Box.createVerticalStrut(10));
        quizPanel.add(questionArea);
        quizPanel.add(Box.createVerticalStrut(10));
        quizPanel.add(optionsPanel);
        quizPanel.add(Box.createVerticalStrut(10));
        quizPanel.add(scoreLabel);
    }

    private void createNetworkPanel() {
        networkPanel = new JPanel(new GridLayout(3, 1, 10, 10));

        hostButton = new JButton("Host Game");
        joinButton = new JButton("Join Game");
        JButton backButton = new JButton("Back to Menu");

        hostButton.addActionListener(e -> hostGame());
        joinButton.addActionListener(e -> joinGame());
        backButton.addActionListener(e -> showMenu());

        networkPanel.add(hostButton);
        networkPanel.add(joinButton);
        networkPanel.add(backButton);
    }

    private void showMenu() {
        cardLayout.show(cardPanel, "MENU");
    }

    private void showQuiz() {
        cardLayout.show(cardPanel, "QUIZ");
    }

    private void showNetworkOptions() {
        cardLayout.show(cardPanel, "NETWORK");
    }

    private void startSoloQuiz() {
        playerName = JOptionPane.showInputDialog(this, "Enter your name:");
        if (playerName == null || playerName.isEmpty()) return;

        Set<String> categories = getCategories();
        if (categories.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No categories found.");
            return;
        }

        String[] categoryArray = categories.toArray(new String[0]);
        String selectedCategory = (String)JOptionPane.showInputDialog(this,
                "Select category:", "Category",
                JOptionPane.PLAIN_MESSAGE, null, categoryArray, categoryArray[0]);

        if (selectedCategory == null) return;

        questions = getQuestionsByCategory(selectedCategory);
        if (questions.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No questions found for this category.");
            return;
        }

        startQuiz();
    }

    private void startApiQuiz() {
        playerName = JOptionPane.showInputDialog(this, "Enter your name:");
        if (playerName == null || playerName.isEmpty()) return;

        questions = fetchQuestionsFromAPI();
        if (questions == null || questions.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Failed to fetch questions from API.");
            return;
        }

        startQuiz();
    }

    private void startQuiz() {
        currentQuestionIndex = 0;
        score = 0;
        scoreLabel.setText("Score: 0");
        statusLabel.setText(playerName + "'s Quiz");
        showQuiz();
        showNextQuestion();
    }

    private void hostGame() {
        playerName = JOptionPane.showInputDialog(this, "Enter your name:");
        if (playerName == null || playerName.isEmpty()) return;

        isHost = true;

        new Thread(() -> {
            try {
                ServerSocket serverSocket = new ServerSocket(5555);
                statusLabel.setText("Waiting for opponent...");
                showQuiz();

                socket = serverSocket.accept();
                out = new PrintWriter(socket.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                // Exchange names
                out.println(playerName);
                opponentName = in.readLine();

                // Host generates questions
                Set<String> categories = getCategories();
                String[] categoryArray = categories.toArray(new String[0]);
                String selectedCategory = (String)JOptionPane.showInputDialog(this,
                        "Select category:", "Category",
                        JOptionPane.PLAIN_MESSAGE, null, categoryArray, categoryArray[0]);

                if (selectedCategory == null) return;

                questions = getQuestionsByCategory(selectedCategory);
                if (questions.isEmpty()) {
                    JOptionPane.showMessageDialog(this, "No questions found for this category.");
                    return;
                }

                // Send questions to client
                out.println("QUESTIONS_START");
                for (Question q : questions) {
                    out.println(q.toString());
                }
                out.println("QUESTIONS_END");

                startPvpGame();
            } catch (IOException e) {
                JOptionPane.showMessageDialog(this, "Error hosting game: " + e.getMessage());
                showMenu();
            }
        }).start();
    }

    private void joinGame() {
        playerName = JOptionPane.showInputDialog(this, "Enter your name:");
        if (playerName == null || playerName.isEmpty()) return;

        String hostIP = JOptionPane.showInputDialog(this, "Enter host IP:", "localhost");
        if (hostIP == null || hostIP.isEmpty()) {
            showMenu();
            return;
        }

        isHost = false;

        new Thread(() -> {
            try {
                socket = new Socket(hostIP, 5555);
                out = new PrintWriter(socket.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                // Exchange names
                out.println(playerName);
                opponentName = in.readLine();

                // Receive questions
                statusLabel.setText("Waiting for questions...");
                showQuiz();

                String line;
                List<Question> receivedQuestions = new ArrayList<>();
                while ((line = in.readLine()) != null) {
                    if (line.equals("QUESTIONS_START")) {
                        receivedQuestions.clear();
                    } else if (line.equals("QUESTIONS_END")) {
                        break;
                    } else {
                        String[] parts = line.split("\\|");
                        if (parts.length >= 3) {
                            List<String> options = Arrays.asList(parts[1].split(","));
                            receivedQuestions.add(new Question(parts[0], options, parts[2]));
                        }
                    }
                }

                questions = receivedQuestions;
                if (questions.isEmpty()) {
                    JOptionPane.showMessageDialog(this, "No questions received.");
                    return;
                }

                startPvpGame();
            } catch (IOException e) {
                JOptionPane.showMessageDialog(this, "Error joining game: " + e.getMessage());
                showMenu();
            }
        }).start();
    }

    private void startPvpGame() {
        SwingUtilities.invokeLater(() -> {
            currentQuestionIndex = 0;
            score = 0;
            scoreLabel.setText("Score: 0");
            statusLabel.setText(playerName + " vs " + opponentName);
            showNextQuestion();
        });
    }

    private void showNextQuestion() {
        if (currentQuestionIndex >= questions.size()) {
            endGame();
            return;
        }

        Question q = questions.get(currentQuestionIndex);
        questionArea.setText(q.getQuestion());

        List<String> options = new ArrayList<>(q.getOptions());
        Collections.shuffle(options);

        for (int i = 0; i < 4; i++) {
            optionButtons[i].setText(options.get(i));
        }

        startTimer();
    }

    private void startTimer() {
        timeLeft = 10;
        timerLabel.setText("Time: " + timeLeft + "s");

        if (questionTimer != null) {
            questionTimer.stop();
        }

        questionTimer = new Timer(1000, e -> {
            timeLeft--;
            timerLabel.setText("Time: " + timeLeft + "s");

            if (timeLeft <= 0) {
                questionTimer.stop();
                handleAnswer(-1); // Timeout
            }
        });

        questionTimer.start();
    }

    private void handleAnswer(int selectedIndex) {
        questionTimer.stop();

        Question q = questions.get(currentQuestionIndex);
        boolean correct = false;

        if (selectedIndex >= 0) {
            String selectedAnswer = optionButtons[selectedIndex].getText();
            correct = q.isCorrect(selectedAnswer);
        }

        if (correct) {
            score++;
            scoreLabel.setText("Score: " + score);
            JOptionPane.showMessageDialog(this, "Correct!");
        } else {
            JOptionPane.showMessageDialog(this, "Wrong! Correct answer: " + q.getCorrectAnswer());
        }

        currentQuestionIndex++;
        showNextQuestion();
    }

    private void endGame() {
        String message;
        if (isHost && socket != null && !socket.isClosed()) {
            try {
                out.println("GAME_OVER|" + score);
                String opponentResult = in.readLine();
                message = "Game Over!\nYour score: " + score + "\n" +
                        opponentName + "'s score: " + opponentResult.split("\\|")[1];
            } catch (IOException e) {
                message = "Game Over! Your score: " + score;
            }
        } else if (!isHost && socket != null && !socket.isClosed()) {
            out.println("GAME_OVER|" + score);
            message = "Game Over! Your score: " + score;
        } else {
            message = "Game Over! Your score: " + score + "/" + questions.size();
        }

        JOptionPane.showMessageDialog(this, message);
        showMenu();

        try {
            if (socket != null) socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private Set<String> getCategories() {
        Set<String> categories = new HashSet<>();
        try (BufferedReader reader = new BufferedReader(new FileReader("questions.txt"))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("Category: ")) {
                    categories.add(line.substring(10).trim());
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return categories;
    }

    private List<Question> getQuestionsByCategory(String category) {
        List<Question> questions = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader("questions.txt"))) {
            String line, currentCategory = "", question = "", answer = "";
            List<String> options = null;

            while ((line = reader.readLine()) != null) {
                if (line.startsWith("Category: ")) {
                    currentCategory = line.substring(10).trim();
                } else if (line.startsWith("Question: ")) {
                    question = line.substring(10).trim();
                } else if (line.startsWith("Options: ")) {
                    options = Arrays.asList(line.substring(9).trim().split("\\|"));
                } else if (line.startsWith("Answer: ")) {
                    answer = line.substring(8).trim();
                    if (currentCategory.equals(category)) {
                        questions.add(new Question(question, options, answer));
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return questions;
    }

    private List<Question> fetchQuestionsFromAPI() {
        try {
            URL url = new URL("https://opentdb.com/api.php?amount=10&type=multiple");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");

            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();

            return parseApiQuestions(response.toString());
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private List<Question> parseApiQuestions(String json) {
        List<Question> questions = new ArrayList<>();
        try {
            String[] questionBlocks = json.split("\\{\"category\":");
            for (int i = 1; i < questionBlocks.length; i++) {
                String block = questionBlocks[i];

                // Extract question
                int qStart = block.indexOf("\"question\":\"") + 12;
                int qEnd = block.indexOf("\"", qStart);
                String question = block.substring(qStart, qEnd)
                        .replace("&quot;", "\"").replace("&#039;", "'");

                // Extract correct answer
                int caStart = block.indexOf("\"correct_answer\":\"") + 17;
                int caEnd = block.indexOf("\"", caStart);
                String correctAnswer = block.substring(caStart, caEnd)
                        .replace("&quot;", "\"").replace("&#039;", "'");

                // Extract incorrect answers
                int iaStart = block.indexOf("\"incorrect_answers\":[\"") + 22;
                int iaEnd = block.indexOf("\"]", iaStart);
                String[] incorrectAnswers = block.substring(iaStart, iaEnd).split("\",\"");

                // Create options list
                List<String> options = new ArrayList<>();
                options.add(correctAnswer);
                for (String ans : incorrectAnswers) {
                    options.add(ans.replace("&quot;", "\"").replace("&#039;", "'"));
                }
                Collections.shuffle(options);

                questions.add(new Question(question, options, correctAnswer));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return questions;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            QuizAppUI app = new QuizAppUI();
            app.setVisible(true);
        });
    }
}

class Question {
    private String question;
    private List<String> options;
    private String correctAnswer;

    public Question(String question, List<String> options, String correctAnswer) {
        this.question = question;
        this.options = options;
        this.correctAnswer = correctAnswer;
    }

    public String getQuestion() {
        return question;
    }

    public List<String> getOptions() {
        return options;
    }

    public String getCorrectAnswer() {
        return correctAnswer;
    }

    public boolean isCorrect(String answer) {
        return answer.equals(correctAnswer);
    }

    @Override
    public String toString() {
        return question + "|" + String.join(",", options) + "|" + correctAnswer;
    }
}