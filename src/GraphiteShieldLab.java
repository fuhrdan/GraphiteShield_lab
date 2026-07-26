import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;

/**
 * GraphiteShield Lab
 * A safe, local-only mobile spyware behavior emulation and detection workbench.
 *
 * This program never connects to, exploits, or collects data from a real device.
 * All displayed devices, identities, data, and telemetry are synthetic.
 */
public class GraphiteShieldLab extends JFrame
{
    private static final Color BG = new Color(9, 14, 20);
    private static final Color PANEL = new Color(16, 24, 33);
    private static final Color PANEL_2 = new Color(22, 33, 44);
    private static final Color CYAN = new Color(45, 212, 191);
    private static final Color BLUE = new Color(56, 189, 248);
    private static final Color AMBER = new Color(251, 191, 36);
    private static final Color RED = new Color(248, 113, 113);
    private static final Color TEXT = new Color(226, 232, 240);
    private static final Color MUTED = new Color(148, 163, 184);

    private final SimpleDateFormat clockFormat = new SimpleDateFormat("HH:mm:ss");
    private final Random random = new Random();
    private final DefaultTableModel eventModel;
    private final DefaultTableModel alertModel;
    private final DefaultTableModel ruleModel;
    private final JTable eventTable;
    private final JTable alertTable;
    private final JTable ruleTable;
    private final JLabel statusLabel = new JLabel("LAB IDLE");
    private final JLabel eventCountLabel = metricValue("0");
    private final JLabel alertCountLabel = metricValue("0");
    private final JLabel riskLabel = metricValue("0 / 100");
    private final JLabel deviceLabel = metricValue("Pixel 9 Lab");
    private final JLabel scenarioStatus = new JLabel("No experiment is running");
    private final JProgressBar riskBar = new JProgressBar(0, 100);
    private final JTextArea detailArea = new JTextArea();
    private final JTextArea logArea = new JTextArea();
    private final JComboBox<String> scenarioBox;
    private final JComboBox<String> deviceBox;
    private final JComboBox<String> speedBox;
    private final JComboBox<String> severityFilter;
    private final JTextField eventSearch = new JTextField();
    private final JCheckBox autoScroll = new JCheckBox("Auto-scroll", true);
    private final JButton runButton = new JButton("Run experiment");
    private final JButton pauseButton = new JButton("Pause");
    private final JButton stopButton = new JButton("Stop & reset");
    private final javax.swing.Timer simulationTimer;
    private final javax.swing.Timer clockTimer;
    private final JLabel clockLabel = new JLabel();

    private int eventCount;
    private int alertCount;
    private int riskScore;
    private int step;
    private boolean running;
    private String selectedScenario = "";
    private final List<SimEvent> allEvents = new ArrayList<>();
    private final List<Detection> detections = new ArrayList<>();

    private static class SimEvent
    {
        String time, source, action, target, severity, detail;
        SimEvent(String time, String source, String action, String target, String severity, String detail)
        {
            this.time = time; this.source = source; this.action = action;
            this.target = target; this.severity = severity; this.detail = detail;
        }
    }

    private static class Detection
    {
        String id, time, rule, severity, status, evidence;
        Detection(String id, String time, String rule, String severity, String status, String evidence)
        {
            this.id = id; this.time = time; this.rule = rule;
            this.severity = severity; this.status = status; this.evidence = evidence;
        }
    }

    public GraphiteShieldLab()
    {
        super("GraphiteShield Lab — Defensive Mobile Research");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1180, 760));
        setSize(1440, 900);
        setLocationRelativeTo(null);

        eventModel = nonEditableModel(new String[]{"Time", "Source", "Action", "Target", "Severity", "Details"});
        alertModel = nonEditableModel(new String[]{"ID", "Time", "Rule", "Severity", "Status", "Evidence"});
        ruleModel = nonEditableModel(new String[]{"Enabled", "Rule", "Category", "Severity", "Description"});
        eventTable = makeTable(eventModel);
        alertTable = makeTable(alertModel);
        ruleTable = makeTable(ruleModel);
        ruleTable.getColumnModel().getColumn(0).setMaxWidth(70);

        scenarioBox = new JComboBox<>(new String[]{
            "Permission escalation chain",
            "Accessibility service misuse",
            "Covert network beaconing",
            "Persistence behavior",
            "Sensitive API access burst",
            "Combined advanced intrusion simulation",
            "Benign baseline activity"
        });
        deviceBox = new JComboBox<>(new String[]{
            "Pixel 9 Lab — Android 16",
            "Galaxy S25 Lab — Android 16",
            "Pixel 7 Legacy — Android 14",
            "Generic AOSP Emulator — Android 15"
        });
        speedBox = new JComboBox<>(new String[]{"0.5×", "1×", "2×", "4×"});
        severityFilter = new JComboBox<>(new String[]{"All severities", "Critical", "High", "Medium", "Low", "Info"});

        simulationTimer = new javax.swing.Timer(850, e -> simulateStep());
        clockTimer = new javax.swing.Timer(1000, e -> clockLabel.setText(new SimpleDateFormat("yyyy-MM-dd  HH:mm:ss").format(new Date())));
        clockTimer.start();

        setJMenuBar(buildMenu());
        setContentPane(buildShell());
        seedRules();
        installActions();
        applyTheme(this);
        updateControls();
    }

    private JMenuBar buildMenu()
    {
        JMenuBar bar = new JMenuBar();
        JMenu file = new JMenu("File");
        JMenuItem newSession = new JMenuItem("New lab session");
        JMenuItem exportCsv = new JMenuItem("Export telemetry CSV");
        JMenuItem exportHtml = new JMenuItem("Export investigation report");
        JMenuItem exit = new JMenuItem("Exit");
        newSession.addActionListener(e -> resetSession());
        exportCsv.addActionListener(e -> exportCsv());
        exportHtml.addActionListener(e -> exportHtml());
        exit.addActionListener(e -> dispose());
        file.add(newSession); file.addSeparator(); file.add(exportCsv); file.add(exportHtml); file.addSeparator(); file.add(exit);

        JMenu lab = new JMenu("Lab");
        JMenuItem run = new JMenuItem("Run selected experiment");
        JMenuItem pause = new JMenuItem("Pause / resume");
        JMenuItem sample = new JMenuItem("Generate sample dataset");
        run.addActionListener(e -> startSimulation());
        pause.addActionListener(e -> togglePause());
        sample.addActionListener(e -> generateSampleDataset());
        lab.add(run); lab.add(pause); lab.add(sample);

        JMenu help = new JMenu("Help");
        JMenuItem guide = new JMenuItem("Quick guide");
        JMenuItem about = new JMenuItem("About GraphiteShield Lab");
        guide.addActionListener(e -> showGuide());
        about.addActionListener(e -> JOptionPane.showMessageDialog(this,
            "GraphiteShield Lab 1.0\n\nA local-only mobile threat emulation and detection workbench.\n" +
            "All telemetry and identities are synthetic. No exploitation code is included.",
            "About", JOptionPane.INFORMATION_MESSAGE));
        help.add(guide); help.add(about);
        bar.add(file); bar.add(lab); bar.add(help);
        return bar;
    }

    private JPanel buildShell()
    {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(BG);
        root.add(buildHeader(), BorderLayout.NORTH);

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Overview", buildOverview());
        tabs.addTab("Experiment Lab", buildLab());
        tabs.addTab("Live Telemetry", buildTelemetry());
        tabs.addTab("Detections", buildDetections());
        tabs.addTab("Detection Rules", buildRules());
        tabs.addTab("Device Profile", buildDeviceProfile());
        tabs.addTab("Reports", buildReports());
        tabs.addTab("Settings & Safety", buildSettings());
        root.add(tabs, BorderLayout.CENTER);
        root.add(buildFooter(), BorderLayout.SOUTH);
        return root;
    }

    private JPanel buildHeader()
    {
        JPanel header = new JPanel(new BorderLayout(18, 0));
        header.setBorder(new EmptyBorder(15, 20, 15, 20));
        header.setBackground(PANEL);

        JPanel brand = new JPanel();
        brand.setOpaque(false);
        brand.setLayout(new BoxLayout(brand, BoxLayout.Y_AXIS));
        JLabel title = new JLabel("◈  GRAPHITESHIELD LAB");
        title.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 22));
        title.setForeground(CYAN);
        JLabel sub = new JLabel("DEFENSIVE MOBILE RESEARCH WORKBENCH");
        sub.setForeground(MUTED);
        sub.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 11));
        brand.add(title); brand.add(Box.createVerticalStrut(3)); brand.add(sub);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 14, 6));
        right.setOpaque(false);
        JLabel safe = badge("● LOCAL-ONLY LAB", CYAN);
        statusLabel.setFont(new Font(Font.MONOSPACED, Font.BOLD, 12));
        statusLabel.setForeground(MUTED);
        clockLabel.setForeground(MUTED);
        clockLabel.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        right.add(safe); right.add(statusLabel); right.add(clockLabel);
        header.add(brand, BorderLayout.WEST);
        header.add(right, BorderLayout.EAST);
        return header;
    }

    private JPanel buildOverview()
    {
        JPanel page = page();
        JPanel metrics = new JPanel(new GridLayout(1, 4, 12, 0));
        metrics.setOpaque(false);
        metrics.add(metricCard("TELEMETRY EVENTS", eventCountLabel, BLUE));
        metrics.add(metricCard("ACTIVE FINDINGS", alertCountLabel, RED));
        metrics.add(metricCard("CURRENT RISK", riskLabel, AMBER));
        metrics.add(metricCard("LAB DEVICE", deviceLabel, CYAN));
        page.add(metrics, BorderLayout.NORTH);

        JPanel center = new JPanel(new GridLayout(1, 2, 14, 0));
        center.setOpaque(false);
        center.setBorder(new EmptyBorder(14, 0, 0, 0));

        JPanel posture = card("SESSION RISK POSTURE");
        posture.setLayout(new BorderLayout(8, 12));
        riskBar.setStringPainted(true);
        riskBar.setValue(0);
        riskBar.setForeground(CYAN);
        riskBar.setBackground(PANEL_2);
        posture.add(riskBar, BorderLayout.NORTH);
        JTextArea postureText = textBlock(
            "This score reflects synthetic behaviors observed during the current lab session.\n\n" +
            "0–24  Normal activity\n25–49  Review recommended\n50–74  Suspicious behavior\n75–100  High-confidence simulated compromise");
        posture.add(postureText, BorderLayout.CENTER);
        JButton investigate = new JButton("Open detection queue");
        investigate.addActionListener(e -> selectTab("Detections"));
        posture.add(investigate, BorderLayout.SOUTH);

        JPanel quick = card("QUICK ACTIONS");
        quick.setLayout(new GridLayout(5, 1, 8, 8));
        quick.add(actionButton("▶  Run a defensive experiment", e -> selectTab("Experiment Lab")));
        quick.add(actionButton("⌁  Generate sample telemetry", e -> generateSampleDataset()));
        quick.add(actionButton("▦  Review detection rules", e -> selectTab("Detection Rules")));
        quick.add(actionButton("⇩  Export investigation report", e -> exportHtml()));
        quick.add(actionButton("?  Open quick guide", e -> showGuide()));
        center.add(posture); center.add(quick);

        JPanel recent = card("RECENT TELEMETRY");
        recent.setLayout(new BorderLayout());
        JTable overviewTable = makeTable(eventModel);
        overviewTable.setSelectionModel(eventTable.getSelectionModel());
        recent.add(new JScrollPane(overviewTable), BorderLayout.CENTER);

        JPanel stacked = new JPanel(new BorderLayout());
        stacked.setOpaque(false);
        stacked.add(center, BorderLayout.NORTH);
        stacked.add(recent, BorderLayout.CENTER);
        page.add(stacked, BorderLayout.CENTER);
        return page;
    }

    private JPanel buildLab()
    {
        JPanel page = page();
        JPanel controls = card("EXPERIMENT CONFIGURATION");
        controls.setLayout(new GridBagLayout());
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(6, 7, 6, 7); g.fill = GridBagConstraints.HORIZONTAL; g.weightx = 1;
        addField(controls, g, 0, "Scenario", scenarioBox);
        addField(controls, g, 1, "Synthetic device", deviceBox);
        addField(controls, g, 2, "Playback speed", speedBox);
        g.gridy = 3; g.gridx = 0; g.gridwidth = 2;
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0)); buttons.setOpaque(false);
        buttons.add(runButton); buttons.add(pauseButton); buttons.add(stopButton);
        controls.add(buttons, g);
        g.gridx = 2; g.gridwidth = 1;
        scenarioStatus.setForeground(MUTED); controls.add(scenarioStatus, g);
        page.add(controls, BorderLayout.NORTH);

        JTabbedPane inner = new JTabbedPane();
        inner.addTab("Scenario description", scenarioDescriptionPanel());
        inner.addTab("Lab console", new JScrollPane(logArea));
        inner.addTab("Safety boundary", safetyPanel());
        page.add(inner, BorderLayout.CENTER);
        return page;
    }

    private JPanel scenarioDescriptionPanel()
    {
        JPanel p = card("WHAT THIS EXPERIMENT DOES");
        p.setLayout(new BorderLayout());
        JTextArea desc = textBlock("");
        Runnable update = () -> desc.setText(descriptionFor((String) scenarioBox.getSelectedItem()));
        scenarioBox.addActionListener(e -> update.run());
        update.run();
        p.add(desc, BorderLayout.CENTER);
        JLabel note = new JLabel("  Every event is generated in memory from synthetic fixtures; no device connection is used.");
        note.setForeground(CYAN);
        p.add(note, BorderLayout.SOUTH);
        return p;
    }

    private JPanel safetyPanel()
    {
        JPanel p = card("ENFORCED SAFETY BOUNDARY");
        p.setLayout(new BorderLayout());
        p.add(textBlock(
            "✓ No network sockets are opened by the simulator\n" +
            "✓ No ADB or physical-device integration\n" +
            "✓ No exploit, persistence, credential, or surveillance implementation\n" +
            "✓ Synthetic identities and content only\n" +
            "✓ Exports contain only the events visible in this application\n" +
            "✓ Experiments are transparent, reversible, and local to this process"), BorderLayout.NORTH);
        return p;
    }

    private JPanel buildTelemetry()
    {
        JPanel page = page();
        JPanel filters = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 5));
        filters.setOpaque(false);
        filters.add(new JLabel("Severity:")); filters.add(severityFilter);
        filters.add(new JLabel("Search:")); eventSearch.setColumns(24); filters.add(eventSearch);
        filters.add(autoScroll);
        JButton clear = new JButton("Clear telemetry");
        clear.addActionListener(e -> clearTelemetry());
        filters.add(clear);
        page.add(filters, BorderLayout.NORTH);

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, new JScrollPane(eventTable), new JScrollPane(detailArea));
        split.setResizeWeight(.72);
        split.setBorder(null);
        page.add(split, BorderLayout.CENTER);
        return page;
    }

    private JPanel buildDetections()
    {
        JPanel page = page();
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 5)); buttons.setOpaque(false);
        JButton acknowledge = new JButton("Acknowledge selected");
        JButton close = new JButton("Close as lab-verified");
        JButton reopen = new JButton("Reopen");
        JButton export = new JButton("Export report");
        acknowledge.addActionListener(e -> setAlertStatus("Acknowledged"));
        close.addActionListener(e -> setAlertStatus("Lab verified"));
        reopen.addActionListener(e -> setAlertStatus("Open"));
        export.addActionListener(e -> exportHtml());
        buttons.add(acknowledge); buttons.add(close); buttons.add(reopen); buttons.add(export);
        page.add(buttons, BorderLayout.NORTH);
        page.add(new JScrollPane(alertTable), BorderLayout.CENTER);
        return page;
    }

    private JPanel buildRules()
    {
        JPanel page = page();
        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 5)); controls.setOpaque(false);
        JButton enable = new JButton("Enable selected");
        JButton disable = new JButton("Disable selected");
        JButton defaults = new JButton("Restore defaults");
        JButton test = new JButton("Test selected rule");
        enable.addActionListener(e -> setRuleEnabled(true));
        disable.addActionListener(e -> setRuleEnabled(false));
        defaults.addActionListener(e -> seedRules());
        test.addActionListener(e -> testRule());
        controls.add(enable); controls.add(disable); controls.add(defaults); controls.add(test);
        page.add(controls, BorderLayout.NORTH);
        page.add(new JScrollPane(ruleTable), BorderLayout.CENTER);
        return page;
    }

    private JPanel buildDeviceProfile()
    {
        JPanel page = page();
        JPanel left = card("SYNTHETIC DEVICE");
        left.setLayout(new GridLayout(0, 2, 10, 10));
        addPair(left, "Profile", deviceBox);
        addPair(left, "Owner identity", new JLabel("Test Subject 0042"));
        addPair(left, "Enrollment", new JLabel("Explicit lab consent"));
        addPair(left, "Network", new JLabel("Simulated Wi-Fi"));
        addPair(left, "Data source", new JLabel("In-memory fixtures"));
        addPair(left, "Real-device access", badge("DISABLED", RED));

        JPanel right = card("TELEMETRY SOURCES");
        right.setLayout(new GridLayout(0, 1, 5, 5));
        String[] sources = {"Application lifecycle", "Permission changes", "Accessibility activity",
            "Synthetic network flow", "Sensitive API calls", "Scheduled-task activity",
            "Package state", "Process behavior"};
        for (String s : sources)
        {
            JCheckBox box = new JCheckBox(s, true);
            right.add(box);
        }
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, left, right);
        split.setResizeWeight(.5); split.setBorder(null);
        page.add(split, BorderLayout.CENTER);
        return page;
    }

    private JPanel buildReports()
    {
        JPanel page = page();
        JPanel options = card("REPORT BUILDER");
        options.setLayout(new GridLayout(0, 1, 6, 6));
        JCheckBox summary = new JCheckBox("Executive summary", true);
        JCheckBox timeline = new JCheckBox("Telemetry timeline", true);
        JCheckBox findings = new JCheckBox("Detection findings and evidence", true);
        JCheckBox rules = new JCheckBox("Detection-rule inventory", true);
        JCheckBox methodology = new JCheckBox("Safety and methodology statement", true);
        options.add(summary); options.add(timeline); options.add(findings); options.add(rules); options.add(methodology);
        JButton html = new JButton("Generate HTML investigation report");
        JButton csv = new JButton("Export telemetry as CSV");
        JButton preview = new JButton("Preview report summary");
        html.addActionListener(e -> exportHtml());
        csv.addActionListener(e -> exportCsv());
        preview.addActionListener(e -> JOptionPane.showMessageDialog(this,
            "Session risk: " + riskScore + "/100\nTelemetry events: " + eventCount +
            "\nDetection findings: " + alertCount + "\nScenario: " +
            (selectedScenario.isEmpty() ? "Not run" : selectedScenario),
            "Report preview", JOptionPane.INFORMATION_MESSAGE));
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT)); buttons.setOpaque(false);
        buttons.add(html); buttons.add(csv); buttons.add(preview);
        options.add(buttons);
        page.add(options, BorderLayout.NORTH);
        page.add(textBlock("Reports are written to a location you select. They contain only synthetic lab telemetry generated during this session."), BorderLayout.CENTER);
        return page;
    }

    private JPanel buildSettings()
    {
        JPanel page = page();
        JPanel settings = card("APPLICATION SETTINGS");
        settings.setLayout(new GridLayout(0, 2, 10, 10));
        addPair(settings, "Theme", new JComboBox<>(new String[]{"Midnight teal"}));
        addPair(settings, "Default severity threshold", new JComboBox<>(new String[]{"Low and above", "Medium and above", "High and above"}));
        addPair(settings, "Maximum retained events", new JSpinner(new SpinnerNumberModel(5000, 100, 50000, 100)));
        addPair(settings, "Timestamp format", new JComboBox<>(new String[]{"Local time", "UTC"}));
        addPair(settings, "Confirm before clearing", new JCheckBox("", true));
        addPair(settings, "Allow real-device connections", badge("PERMANENTLY DISABLED", RED));
        page.add(settings, BorderLayout.NORTH);
        page.add(safetyPanel(), BorderLayout.CENTER);
        return page;
    }

    private JPanel buildFooter()
    {
        JPanel p = new JPanel(new BorderLayout());
        p.setBorder(new EmptyBorder(6, 14, 6, 14)); p.setBackground(PANEL);
        JLabel left = new JLabel("GraphiteShield Lab 1.0  •  Open-source defensive research prototype");
        JLabel right = new JLabel("SYNTHETIC DATA ONLY");
        left.setForeground(MUTED); right.setForeground(CYAN);
        p.add(left, BorderLayout.WEST); p.add(right, BorderLayout.EAST);
        return p;
    }

    private void installActions()
    {
        runButton.addActionListener(e -> startSimulation());
        pauseButton.addActionListener(e -> togglePause());
        stopButton.addActionListener(e -> stopSimulation());
        speedBox.addActionListener(e -> updateTimerSpeed());
        deviceBox.addActionListener(e -> deviceLabel.setText(((String) deviceBox.getSelectedItem()).split(" — ")[0]));
        eventTable.getSelectionModel().addListSelectionListener(e -> showSelectedEvent());
        alertTable.getSelectionModel().addListSelectionListener(e -> showSelectedAlert());
        eventSearch.getDocument().addDocumentListener(new DocumentListener()
        {
            public void insertUpdate(DocumentEvent e) { applyEventFilter(); }
            public void removeUpdate(DocumentEvent e) { applyEventFilter(); }
            public void changedUpdate(DocumentEvent e) { applyEventFilter(); }
        });
        severityFilter.addActionListener(e -> applyEventFilter());
    }

    private void startSimulation()
    {
        if (!running)
        {
            if (step == 0)
            {
                selectedScenario = (String) scenarioBox.getSelectedItem();
                log("Experiment started: " + selectedScenario);
                addEvent("lab.controller", "SESSION_START", "experiment", "Info", "Authorized synthetic experiment initialized");
            }
            running = true;
            simulationTimer.start();
            statusLabel.setText("● LAB RUNNING");
            statusLabel.setForeground(CYAN);
            scenarioStatus.setText("Running: " + selectedScenario);
            updateControls();
        }
    }

    private void togglePause()
    {
        if (running)
        {
            running = false;
            simulationTimer.stop();
            statusLabel.setText("LAB PAUSED");
            statusLabel.setForeground(AMBER);
            scenarioStatus.setText("Paused at event step " + step);
        }
        else if (step > 0)
        {
            running = true;
            simulationTimer.start();
            statusLabel.setText("● LAB RUNNING");
            statusLabel.setForeground(CYAN);
            scenarioStatus.setText("Running: " + selectedScenario);
        }
        updateControls();
    }

    private void stopSimulation()
    {
        simulationTimer.stop(); running = false; step = 0;
        statusLabel.setText("LAB IDLE"); statusLabel.setForeground(MUTED);
        scenarioStatus.setText("Experiment stopped; telemetry retained");
        log("Experiment stopped by researcher.");
        updateControls();
    }

    private void simulateStep()
    {
        step++;
        String scenario = selectedScenario;
        if (scenario.contains("Benign"))
        {
            benignEvent();
            if (step >= 14) finishSimulation();
            return;
        }

        if (scenario.contains("Permission") || scenario.contains("Combined")) permissionStep(step);
        if (scenario.contains("Accessibility") || scenario.contains("Combined")) accessibilityStep(step);
        if (scenario.contains("network") || scenario.contains("Combined")) networkStep(step);
        if (scenario.contains("Persistence") || scenario.contains("Combined")) persistenceStep(step);
        if (scenario.contains("Sensitive") || scenario.contains("Combined")) sensitiveStep(step);
        if (!scenario.contains("Combined") && step % 2 == 0) benignEvent();
        if (step >= (scenario.contains("Combined") ? 18 : 12)) finishSimulation();
    }

    private void permissionStep(int s)
    {
        if (s == 2) addEvent("com.lab.fixture", "PERMISSION_REQUEST", "READ_CONTACTS", "Low", "Synthetic application requested contacts permission");
        if (s == 4) addEvent("package.manager", "PERMISSION_GRANTED", "READ_CONTACTS", "Medium", "Permission granted outside expected onboarding sequence");
        if (s == 6)
        {
            addEvent("com.lab.fixture", "PERMISSION_REQUEST", "RECORD_AUDIO", "High", "Sensitive permission requested while app is backgrounded");
            detect("GS-PERM-003", "Background sensitive-permission escalation", "High", "RECORD_AUDIO request followed contacts grant");
        }
    }

    private void accessibilityStep(int s)
    {
        if (s == 3) addEvent("accessibility.manager", "SERVICE_ENABLED", "LabAssistService", "Medium", "Synthetic accessibility service enabled");
        if (s == 6) addEvent("LabAssistService", "WINDOW_ENUMERATION", "messaging.fixture", "High", "Rapid synthetic UI window enumeration");
        if (s == 8)
        {
            addEvent("LabAssistService", "GESTURE_INJECTION", "settings.fixture", "Critical", "Synthetic gesture pattern attempted");
            detect("GS-ACC-001", "Accessibility automation abuse", "Critical", "Service enumeration followed synthetic gesture injection");
        }
    }

    private void networkStep(int s)
    {
        if (s == 2 || s == 5 || s == 8)
            addEvent("net.fixture", "TLS_BEACON", "198.51.100.42:443", s == 8 ? "High" : "Medium", "Synthetic 512-byte periodic callback");
        if (s == 8) detect("GS-NET-002", "Regular covert beacon interval", "High", "Three equally spaced synthetic callbacks to TEST-NET-2");
    }

    private void persistenceStep(int s)
    {
        if (s == 3) addEvent("scheduler.fixture", "JOB_CREATED", "LabKeepAlive", "Medium", "Synthetic background job scheduled");
        if (s == 6) addEvent("boot.fixture", "RECEIVER_REGISTERED", "BOOT_COMPLETED", "High", "Synthetic package registered restart trigger");
        if (s == 7)
        {
            addEvent("battery.fixture", "OPTIMIZATION_EXEMPT", "com.lab.fixture", "High", "Synthetic battery exemption observed");
            detect("GS-PER-004", "Layered persistence behavior", "High", "Job + boot receiver + power exemption sequence");
        }
    }

    private void sensitiveStep(int s)
    {
        String[] targets = {"CONTACTS_FIXTURE", "LOCATION_FIXTURE", "CALENDAR_FIXTURE", "MICROPHONE_FIXTURE"};
        if (s >= 3 && s <= 7)
            addEvent("api.fixture", "SENSITIVE_API_ACCESS", targets[(s - 3) % targets.length], s >= 6 ? "High" : "Medium", "Synthetic API access recorded");
        if (s == 7) detect("GS-API-006", "Sensitive API access burst", "High", "Four synthetic sensitive sources accessed in under five seconds");
    }

    private void benignEvent()
    {
        String[][] values = {
            {"system.ui", "SCREEN_ON", "display", "Info", "Normal synthetic user activity"},
            {"mail.fixture", "SYNC", "mailbox.fixture", "Info", "Scheduled benign synchronization"},
            {"weather.fixture", "API_CALL", "weather.test", "Low", "Expected foreground network request"},
            {"launcher.fixture", "APP_START", "notes.fixture", "Info", "Synthetic application opened by user"},
            {"update.fixture", "PACKAGE_CHECK", "repository.test", "Info", "Expected package metadata check"}
        };
        String[] v = values[random.nextInt(values.length)];
        addEvent(v[0], v[1], v[2], v[3], v[4]);
    }

    private void finishSimulation()
    {
        simulationTimer.stop(); running = false;
        addEvent("lab.controller", "SESSION_COMPLETE", "experiment", "Info", "Synthetic experiment completed normally");
        log("Experiment completed. Risk score: " + riskScore + "/100.");
        statusLabel.setText("LAB COMPLETE"); statusLabel.setForeground(BLUE);
        scenarioStatus.setText("Complete — review telemetry and detections");
        updateControls();
    }

    private void addEvent(String source, String action, String target, String severity, String detail)
    {
        String now = clockFormat.format(new Date());
        SimEvent event = new SimEvent(now, source, action, target, severity, detail);
        allEvents.add(event);
        eventModel.addRow(new Object[]{now, source, action, target, severity, detail});
        eventCount++;
        eventCountLabel.setText(String.valueOf(eventCount));
        if (autoScroll.isSelected())
        {
            int last = eventTable.getRowCount() - 1;
            if (last >= 0) eventTable.scrollRectToVisible(eventTable.getCellRect(last, 0, true));
        }
    }

    private void detect(String id, String rule, String severity, String evidence)
    {
        if (!isRuleEnabled(rule)) return;
        String now = clockFormat.format(new Date());
        Detection d = new Detection(id, now, rule, severity, "Open", evidence);
        detections.add(d);
        alertModel.addRow(new Object[]{id, now, rule, severity, "Open", evidence});
        alertCount++; alertCountLabel.setText(String.valueOf(alertCount));
        int addition = severity.equals("Critical") ? 28 : severity.equals("High") ? 18 : 10;
        riskScore = Math.min(100, riskScore + addition);
        riskLabel.setText(riskScore + " / 100"); riskBar.setValue(riskScore);
        riskBar.setForeground(riskScore >= 75 ? RED : riskScore >= 45 ? AMBER : CYAN);
        log("DETECTION " + id + ": " + rule + " [" + severity + "]");
    }

    private boolean isRuleEnabled(String rule)
    {
        for (int i = 0; i < ruleModel.getRowCount(); i++)
            if (ruleModel.getValueAt(i, 1).toString().equals(rule))
                return Boolean.TRUE.equals(ruleModel.getValueAt(i, 0));
        return true;
    }

    private void seedRules()
    {
        ruleModel.setRowCount(0);
        addRule(true, "Background sensitive-permission escalation", "Permissions", "High", "Sensitive permission requested from background after another grant");
        addRule(true, "Accessibility automation abuse", "Accessibility", "Critical", "Window enumeration followed by synthetic gesture injection");
        addRule(true, "Regular covert beacon interval", "Network", "High", "Periodic fixed-size callbacks with low timing variance");
        addRule(true, "Layered persistence behavior", "Persistence", "High", "Scheduled job, boot receiver, and power exemption combination");
        addRule(true, "Sensitive API access burst", "Data access", "High", "Multiple sensitive data categories accessed rapidly");
        addRule(true, "Unusual foreground-service start", "Process", "Medium", "Foreground service started without a user-visible action");
        addRule(true, "Unexpected package-state change", "Package", "Medium", "Package component enabled outside installation flow");
        addRule(false, "Aggressive location sampling", "Location", "Low", "Location sample rate exceeds lab baseline");
    }

    private void addRule(boolean enabled, String rule, String category, String severity, String description)
    {
        ruleModel.addRow(new Object[]{enabled, rule, category, severity, description});
    }

    private void generateSampleDataset()
    {
        for (int i = 0; i < 12; i++) benignEvent();
        addEvent("net.fixture", "TLS_BEACON", "198.51.100.42:443", "High", "Synthetic periodic callback");
        addEvent("api.fixture", "SENSITIVE_API_ACCESS", "CONTACTS_FIXTURE", "Medium", "Synthetic API access recorded");
        detect("GS-SAMPLE-001", "Regular covert beacon interval", "High", "Generated demonstration evidence");
        JOptionPane.showMessageDialog(this, "A synthetic sample dataset was added.", "Dataset generated", JOptionPane.INFORMATION_MESSAGE);
    }

    private void setAlertStatus(String status)
    {
        int row = alertTable.getSelectedRow();
        if (row < 0) { selectRequired("Select a detection first."); return; }
        int modelRow = alertTable.convertRowIndexToModel(row);
        alertModel.setValueAt(status, modelRow, 4);
        detections.get(modelRow).status = status;
    }

    private void setRuleEnabled(boolean enabled)
    {
        int row = ruleTable.getSelectedRow();
        if (row < 0) { selectRequired("Select a rule first."); return; }
        ruleModel.setValueAt(enabled, ruleTable.convertRowIndexToModel(row), 0);
    }

    private void testRule()
    {
        int row = ruleTable.getSelectedRow();
        if (row < 0) { selectRequired("Select a rule first."); return; }
        int m = ruleTable.convertRowIndexToModel(row);
        String rule = ruleModel.getValueAt(m, 1).toString();
        if (!Boolean.TRUE.equals(ruleModel.getValueAt(m, 0)))
        {
            JOptionPane.showMessageDialog(this, "Enable the rule before testing it.", "Rule disabled", JOptionPane.WARNING_MESSAGE);
            return;
        }
        addEvent("rule.test", "FIXTURE_MATCH", rule, "Info", "Synthetic unit-test fixture evaluated");
        detect("GS-TEST-" + String.format("%03d", alertCount + 1), rule, ruleModel.getValueAt(m, 3).toString(), "Manual rule-test fixture");
    }

    private void applyEventFilter()
    {
        TableRowSorter<TableModel> sorter = new TableRowSorter<>(eventModel);
        List<RowFilter<Object,Object>> filters = new ArrayList<>();
        String severity = (String) severityFilter.getSelectedItem();
        if (severity != null && !severity.startsWith("All"))
            filters.add(RowFilter.regexFilter("^" + severity + "$", 4));
        String search = eventSearch.getText().trim();
        if (!search.isEmpty())
            filters.add(RowFilter.regexFilter("(?i)" + java.util.regex.Pattern.quote(search)));
        if (!filters.isEmpty()) sorter.setRowFilter(RowFilter.andFilter(filters));
        eventTable.setRowSorter(sorter);
    }

    private void showSelectedEvent()
    {
        int row = eventTable.getSelectedRow();
        if (row < 0) return;
        int m = eventTable.convertRowIndexToModel(row);
        detailArea.setText("EVENT INSPECTOR\n\nTimestamp: " + eventModel.getValueAt(m, 0) +
            "\nSource: " + eventModel.getValueAt(m, 1) + "\nAction: " + eventModel.getValueAt(m, 2) +
            "\nTarget: " + eventModel.getValueAt(m, 3) + "\nSeverity: " + eventModel.getValueAt(m, 4) +
            "\n\nAnalysis\n" + eventModel.getValueAt(m, 5) +
            "\n\nProvenance\nGenerated locally by the GraphiteShield synthetic fixture engine.");
    }

    private void showSelectedAlert()
    {
        int row = alertTable.getSelectedRow();
        if (row < 0) return;
        int m = alertTable.convertRowIndexToModel(row);
        alertTable.setToolTipText(alertModel.getValueAt(m, 5).toString());
    }

    private void clearTelemetry()
    {
        if (JOptionPane.showConfirmDialog(this, "Clear all session telemetry and detections?", "Clear session",
            JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) == JOptionPane.YES_OPTION) resetSession();
    }

    private void resetSession()
    {
        simulationTimer.stop(); running = false; step = 0; eventCount = 0; alertCount = 0; riskScore = 0;
        allEvents.clear(); detections.clear(); eventModel.setRowCount(0); alertModel.setRowCount(0);
        eventCountLabel.setText("0"); alertCountLabel.setText("0"); riskLabel.setText("0 / 100"); riskBar.setValue(0);
        statusLabel.setText("LAB IDLE"); statusLabel.setForeground(MUTED); scenarioStatus.setText("No experiment is running");
        logArea.setText(""); detailArea.setText(""); selectedScenario = ""; updateControls();
    }

    private void exportCsv()
    {
        JFileChooser chooser = saveChooser("graphiteshield-telemetry.csv");
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
        try (BufferedWriter w = Files.newBufferedWriter(chooser.getSelectedFile().toPath(), StandardCharsets.UTF_8))
        {
            w.write("time,source,action,target,severity,details\n");
            for (SimEvent e : allEvents)
                w.write(csv(e.time) + "," + csv(e.source) + "," + csv(e.action) + "," + csv(e.target) + "," + csv(e.severity) + "," + csv(e.detail) + "\n");
            exportSuccess(chooser.getSelectedFile());
        }
        catch (IOException ex) { exportError(ex); }
    }

    private void exportHtml()
    {
        JFileChooser chooser = saveChooser("graphiteshield-report.html");
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
        StringBuilder h = new StringBuilder();
        h.append("<!doctype html><html><head><meta charset='utf-8'><title>GraphiteShield Report</title>");
        h.append("<style>body{font:15px system-ui;background:#091016;color:#dce7ef;margin:40px}h1,h2{color:#2dd4bf}.card{background:#101821;padding:20px;border:1px solid #263747;border-radius:10px;margin:16px 0}table{width:100%;border-collapse:collapse}th,td{text-align:left;padding:8px;border-bottom:1px solid #263747}th{color:#38bdf8}.high,.critical{color:#f87171}.medium{color:#fbbf24}.note{color:#94a3b8}</style></head><body>");
        h.append("<h1>GraphiteShield Lab Investigation Report</h1><p class='note'>Generated ").append(esc(new Date().toString())).append(" • Synthetic lab data only</p>");
        h.append("<div class='card'><h2>Session Summary</h2><p>Scenario: ").append(esc(selectedScenario.isEmpty() ? "Sample / manual session" : selectedScenario));
        h.append("</p><p>Risk score: ").append(riskScore).append("/100 &nbsp; Events: ").append(eventCount).append(" &nbsp; Findings: ").append(alertCount).append("</p></div>");
        h.append("<div class='card'><h2>Findings</h2><table><tr><th>ID</th><th>Time</th><th>Rule</th><th>Severity</th><th>Status</th><th>Evidence</th></tr>");
        for (Detection d : detections)
            h.append("<tr><td>").append(esc(d.id)).append("</td><td>").append(esc(d.time)).append("</td><td>").append(esc(d.rule)).append("</td><td class='").append(d.severity.toLowerCase()).append("'>").append(esc(d.severity)).append("</td><td>").append(esc(d.status)).append("</td><td>").append(esc(d.evidence)).append("</td></tr>");
        h.append("</table></div><div class='card'><h2>Telemetry Timeline</h2><table><tr><th>Time</th><th>Source</th><th>Action</th><th>Target</th><th>Severity</th><th>Details</th></tr>");
        for (SimEvent e : allEvents)
            h.append("<tr><td>").append(esc(e.time)).append("</td><td>").append(esc(e.source)).append("</td><td>").append(esc(e.action)).append("</td><td>").append(esc(e.target)).append("</td><td>").append(esc(e.severity)).append("</td><td>").append(esc(e.detail)).append("</td></tr>");
        h.append("</table></div><div class='card'><h2>Methodology & Safety</h2><p>This report was produced by a transparent, local-only behavior simulator. No physical device was connected, no exploit was executed, and no personal data was accessed. All identities, endpoints, and events are synthetic fixtures.</p></div></body></html>");
        try
        {
            Files.writeString(chooser.getSelectedFile().toPath(), h.toString(), StandardCharsets.UTF_8);
            exportSuccess(chooser.getSelectedFile());
        }
        catch (IOException ex) { exportError(ex); }
    }

    private void updateTimerSpeed()
    {
        String speed = (String) speedBox.getSelectedItem();
        int delay = speed.startsWith("0.5") ? 1700 : speed.startsWith("2") ? 425 : speed.startsWith("4") ? 215 : 850;
        simulationTimer.setDelay(delay);
    }

    private void updateControls()
    {
        runButton.setEnabled(!running && step == 0);
        pauseButton.setEnabled(running || step > 0);
        pauseButton.setText(running ? "Pause" : (step > 0 ? "Resume" : "Pause"));
        stopButton.setEnabled(running || step > 0);
        scenarioBox.setEnabled(step == 0);
        deviceBox.setEnabled(step == 0);
    }

    private void selectTab(String name)
    {
        JTabbedPane tabs = findTabs(getContentPane());
        if (tabs != null) for (int i = 0; i < tabs.getTabCount(); i++) if (name.equals(tabs.getTitleAt(i))) tabs.setSelectedIndex(i);
    }

    private JTabbedPane findTabs(Container c)
    {
        for (Component x : c.getComponents())
        {
            if (x instanceof JTabbedPane) return (JTabbedPane) x;
            if (x instanceof Container)
            {
                JTabbedPane t = findTabs((Container) x); if (t != null) return t;
            }
        }
        return null;
    }

    private String descriptionFor(String scenario)
    {
        if (scenario.startsWith("Permission")) return "Models a sequence in which a synthetic application requests increasingly sensitive permissions outside its expected lifecycle. The detector correlates timing, application state, and permission sensitivity.";
        if (scenario.startsWith("Accessibility")) return "Models suspicious use of an explicitly enabled synthetic accessibility service. It emits window-enumeration and gesture fixtures so defensive rules can be tested without interacting with the operating system.";
        if (scenario.startsWith("Covert")) return "Generates synthetic, non-routable flow records with regular timing and fixed payload sizes. No socket is opened. The detector looks for low-variance beacon behavior.";
        if (scenario.startsWith("Persistence")) return "Models a layered persistence pattern through synthetic scheduler, boot-receiver, and battery-exemption events. It does not alter the host computer or a mobile device.";
        if (scenario.startsWith("Sensitive")) return "Generates a burst of synthetic access records representing contacts, location, calendar, and microphone APIs. No real API or personal information is accessed.";
        if (scenario.startsWith("Combined")) return "Runs all five synthetic behavior families together to demonstrate correlation, prioritization, evidence review, and report generation under a noisy advanced-threat scenario.";
        return "Generates routine foreground use, synchronization, updates, and expected network activity. This baseline should produce few or no findings and can be used to evaluate false-positive behavior.";
    }

    private static DefaultTableModel nonEditableModel(String[] columns)
    {
        return new DefaultTableModel(columns, 0)
        {
            public boolean isCellEditable(int row, int column) { return false; }
            public Class<?> getColumnClass(int column) { return column == 0 && getColumnName(column).equals("Enabled") ? Boolean.class : String.class; }
        };
    }

    private static JTable makeTable(TableModel model)
    {
        JTable t = new JTable(model);
        t.setAutoCreateRowSorter(true); t.setRowHeight(27); t.setFillsViewportHeight(true);
        t.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        return t;
    }

    private static JPanel page()
    {
        JPanel p = new JPanel(new BorderLayout(0, 10));
        p.setBorder(new EmptyBorder(14, 14, 14, 14)); p.setBackground(BG);
        return p;
    }

    private static JPanel card(String title)
    {
        JPanel p = new JPanel();
        p.setBackground(PANEL);
        p.setBorder(new CompoundBorder(new LineBorder(new Color(40, 57, 72)), new TitledBorder(
            new EmptyBorder(10, 12, 12, 12), title, TitledBorder.LEFT, TitledBorder.TOP,
            new Font(Font.MONOSPACED, Font.BOLD, 12), BLUE)));
        return p;
    }

    private static JLabel metricValue(String value)
    {
        JLabel l = new JLabel(value);
        l.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 25)); l.setForeground(TEXT);
        return l;
    }

    private static JPanel metricCard(String title, JLabel value, Color accent)
    {
        JPanel p = new JPanel(); p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBackground(PANEL); p.setBorder(new CompoundBorder(new MatteBorder(0, 3, 0, 0, accent), new EmptyBorder(13, 16, 13, 12)));
        JLabel t = new JLabel(title); t.setForeground(MUTED); t.setFont(new Font(Font.MONOSPACED, Font.BOLD, 11));
        p.add(t); p.add(Box.createVerticalStrut(8)); p.add(value);
        return p;
    }

    private static JLabel badge(String text, Color color)
    {
        JLabel l = new JLabel(" " + text + " "); l.setForeground(color);
        l.setFont(new Font(Font.MONOSPACED, Font.BOLD, 11)); l.setBorder(new LineBorder(color));
        return l;
    }

    private static JTextArea textBlock(String text)
    {
        JTextArea a = new JTextArea(text);
        a.setEditable(false); a.setLineWrap(true); a.setWrapStyleWord(true); a.setOpaque(false);
        a.setForeground(TEXT); a.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
        a.setBorder(new EmptyBorder(12, 12, 12, 12));
        return a;
    }

    private static JButton actionButton(String text, ActionListener action)
    {
        JButton b = new JButton(text); b.setHorizontalAlignment(SwingConstants.LEFT); b.addActionListener(action); return b;
    }

    private static void addField(JPanel p, GridBagConstraints g, int row, String label, JComponent value)
    {
        g.gridy = row; g.gridx = 0; g.gridwidth = 1; g.weightx = .25; p.add(new JLabel(label), g);
        g.gridx = 1; g.gridwidth = 2; g.weightx = .75; p.add(value, g);
    }

    private static void addPair(JPanel p, String label, JComponent value)
    {
        JLabel l = new JLabel(label); l.setForeground(MUTED); p.add(l); p.add(value);
    }

    private void log(String text)
    {
        logArea.append("[" + clockFormat.format(new Date()) + "] " + text + "\n");
        logArea.setCaretPosition(logArea.getDocument().getLength());
    }

    private static String csv(String value) { return "\"" + value.replace("\"", "\"\"") + "\""; }
    private static String esc(String value) { return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;"); }

    private JFileChooser saveChooser(String name)
    {
        JFileChooser c = new JFileChooser(); c.setSelectedFile(new File(name)); return c;
    }

    private void exportSuccess(File file)
    {
        JOptionPane.showMessageDialog(this, "Saved:\n" + file.getAbsolutePath(), "Export complete", JOptionPane.INFORMATION_MESSAGE);
    }

    private void exportError(Exception ex)
    {
        JOptionPane.showMessageDialog(this, "Could not export the file:\n" + ex.getMessage(), "Export failed", JOptionPane.ERROR_MESSAGE);
    }

    private void selectRequired(String message)
    {
        JOptionPane.showMessageDialog(this, message, "Selection required", JOptionPane.WARNING_MESSAGE);
    }

    private void showGuide()
    {
        JOptionPane.showMessageDialog(this,
            "1. Open Experiment Lab and select a scenario.\n" +
            "2. Choose a synthetic device and playback speed.\n" +
            "3. Run the experiment and watch Live Telemetry.\n" +
            "4. Review and acknowledge findings in Detections.\n" +
            "5. Enable, disable, or test rules in Detection Rules.\n" +
            "6. Export an HTML report or CSV timeline.\n\n" +
            "Tip: The combined scenario demonstrates the widest range of features.",
            "Quick guide", JOptionPane.INFORMATION_MESSAGE);
    }

    private static void applyTheme(Component c)
    {
        if (c instanceof JPanel || c instanceof JViewport || c instanceof JTabbedPane) c.setBackground(c instanceof JPanel && ((JPanel)c).isOpaque() ? c.getBackground() : BG);
        if (c instanceof JLabel || c instanceof JCheckBox || c instanceof JRadioButton) c.setForeground(TEXT);
        if (c instanceof JTextArea)
        {
            c.setBackground(PANEL_2); c.setForeground(TEXT);
            ((JTextArea)c).setCaretColor(CYAN);
        }
        if (c instanceof JTextField || c instanceof JComboBox || c instanceof JSpinner)
        {
            c.setBackground(PANEL_2); c.setForeground(TEXT);
        }
        if (c instanceof JTable)
        {
            JTable t = (JTable)c; t.setBackground(PANEL); t.setForeground(TEXT);
            t.setGridColor(new Color(40, 57, 72)); t.setSelectionBackground(new Color(20, 92, 102));
            t.getTableHeader().setBackground(PANEL_2); t.getTableHeader().setForeground(BLUE);
        }
        if (c instanceof JButton)
        {
            c.setBackground(PANEL_2); c.setForeground(TEXT); ((JButton)c).setFocusPainted(false);
        }
        if (c instanceof Container) for (Component child : ((Container)c).getComponents()) applyTheme(child);
    }

    public static void main(String[] args)
    {
        SwingUtilities.invokeLater(() ->
        {
            try { UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName()); }
            catch (Exception ignored) { }
            new GraphiteShieldLab().setVisible(true);
        });
    }
}
