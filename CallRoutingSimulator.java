import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.*;

class Caller {
    String callerId;
    String callerName;
    String callType;
    String department;

    Caller(String callerId, String callerName, String callType, String department) {
        this.callerId = callerId;
        this.callerName = callerName;
        this.callType = callType;
        this.department = department;
    }

    int getPriority() {
        if (callType.equalsIgnoreCase("Emergency")) return 1;
        if (callType.equalsIgnoreCase("VIP")) return 2;
        return 3;
    }

    public String displayCaller() {
        return callerName + " [" + callType + " | " + department + "]";
    }
}

class Agent {
    String agentId;
    String agentName;
    String department;
    boolean isAvailable;

    Agent(String agentId, String agentName, String department) {
        this.agentId = agentId;
        this.agentName = agentName;
        this.department = department;
        this.isAvailable = true;
    }

    void receiveCall() {
        this.isAvailable = false;
    }

    void endCall() {
        this.isAvailable = true;
    }
}

class Call {
    String callId;
    Caller caller;
    Agent agent;
    long startTime;
    long endTime;
    String status;

    Call(String callId, Caller caller, Agent agent) {
        this.callId = callId;
        this.caller = caller;
        this.agent = agent;
        this.status = "Active";
    }

    void startCall() {
        this.startTime = System.currentTimeMillis();
        this.status = "Active";
    }

    void finishCall() {
        this.endTime = System.currentTimeMillis();
        this.status = "Completed";
    }
}

class CallLogNode {
    Call call;
    CallLogNode next;

    CallLogNode(Call call) {
        this.call = call;
        this.next = null;
    }
}

class CallLog {
    CallLogNode head;

    CallLog() {
        head = null;
    }

    void addLog(Call call) {
        CallLogNode newNode = new CallLogNode(call);
        if (head == null) {
            head = newNode;
        } else {
            CallLogNode temp = head;
            while (temp.next != null) temp = temp.next;
            temp.next = newNode;
        }
    }

    ArrayList<Call> getAllLogs() {
        ArrayList<Call> logs = new ArrayList<>();
        CallLogNode temp = head;
        while (temp != null) {
            logs.add(temp.call);
            temp = temp.next;
        }
        return logs;
    }
}

class ActiveCallNode {
    Call call;
    ActiveCallNode prev;
    ActiveCallNode next;

    ActiveCallNode(Call call) {
        this.call = call;
        this.prev = null;
        this.next = null;
    }
}

class ActiveCallList {
    ActiveCallNode head;
    ActiveCallNode tail;

    ActiveCallList() {
        head = null;
        tail = null;
    }

    void addCall(Call call) {
        ActiveCallNode newNode = new ActiveCallNode(call);
        if (head == null) {
            head = newNode;
            tail = newNode;
        } else {
            tail.next = newNode;
            newNode.prev = tail;
            tail = newNode;
        }
    }

    void removeCall(String callId) {
        ActiveCallNode temp = head;
        while (temp != null) {
            if (temp.call.callId.equals(callId)) {
                if (temp.prev != null) temp.prev.next = temp.next;
                else head = temp.next;
                if (temp.next != null) temp.next.prev = temp.prev;
                else tail = temp.prev;
                return;
            }
            temp = temp.next;
        }
    }

    Call findCall(String callId) {
        ActiveCallNode temp = head;
        while (temp != null) {
            if (temp.call.callId.equals(callId)) return temp.call;
            temp = temp.next;
        }
        return null;
    }

    ArrayList<Call> getAllCalls() {
        ArrayList<Call> calls = new ArrayList<>();
        ActiveCallNode temp = head;
        while (temp != null) {
            calls.add(temp.call);
            temp = temp.next;
        }
        return calls;
    }
}

class AgentTreeNode {
    String department;
    ArrayList<Agent> agents;
    ArrayList<AgentTreeNode> children;

    AgentTreeNode(String department) {
        this.department = department;
        this.agents = new ArrayList<>();
        this.children = new ArrayList<>();
    }
}

class AgentTree {
    AgentTreeNode root;

    AgentTree() {
        root = new AgentTreeNode("Company");
        root.children.add(new AgentTreeNode("Sales"));
        root.children.add(new AgentTreeNode("Support"));
        root.children.add(new AgentTreeNode("Billing"));
    }

    void addAgent(Agent agent) {
        for (AgentTreeNode child : root.children) {
            if (child.department.equalsIgnoreCase(agent.department)) {
                child.agents.add(agent);
                return;
            }
        }
        AgentTreeNode newDept = new AgentTreeNode(agent.department);
        newDept.agents.add(agent);
        root.children.add(newDept);
    }

    Agent findAvailableAgent(String department) {
        for (AgentTreeNode child : root.children) {
            if (child.department.equalsIgnoreCase(department)) {
                for (Agent a : child.agents) {
                    if (a.isAvailable) return a;
                }
            }
        }
        return null;
    }

    ArrayList<Agent> getAllAgents() {
        ArrayList<Agent> all = new ArrayList<>();
        for (AgentTreeNode child : root.children) {
            all.addAll(child.agents);
        }
        return all;
    }
}

class RoutingSystem {
    AgentTree agentTree;
    PriorityQueue<Caller> priorityHeap;
    Queue<Caller> waitingQueue;
    ActiveCallList activeCalls;
    CallLog callLog;
    Stack<Call> recentEndedCalls;
    int callCounter = 1;

    RoutingSystem() {
        agentTree = new AgentTree();
        priorityHeap = new PriorityQueue<>((a, b) -> a.getPriority() - b.getPriority());
        waitingQueue = new LinkedList<>();
        activeCalls = new ActiveCallList();
        callLog = new CallLog();
        recentEndedCalls = new Stack<>();

        agentTree.addAgent(new Agent("A001", "Ahmed", "Sales"));
        agentTree.addAgent(new Agent("A002", "Sara", "Sales"));
        agentTree.addAgent(new Agent("A003", "Usman", "Support"));
        agentTree.addAgent(new Agent("A004", "Ayesha", "Support"));
        agentTree.addAgent(new Agent("A005", "Hassan", "Billing"));
        agentTree.addAgent(new Agent("A006", "Zara", "Billing"));
    }

    String routeCall(Caller caller) {
        if (caller.callType.equalsIgnoreCase("Emergency") || caller.callType.equalsIgnoreCase("VIP")) {
            priorityHeap.add(caller);
        } else {
            waitingQueue.add(caller);
        }
        return assignCall();
    }

    String assignCall() {
        Caller next = null;
        if (!priorityHeap.isEmpty()) {
            next = priorityHeap.poll();
        } else if (!waitingQueue.isEmpty()) {
            next = waitingQueue.poll();
        } else {
            return "Call added to queue. No callers to assign.";
        }

        Agent agent = agentTree.findAvailableAgent(next.department);
        if (agent == null) {
            if (next.getPriority() == 1 || next.getPriority() == 2) {
                priorityHeap.add(next);
            } else {
                waitingQueue.add(next);
            }
            return "No agent available for " + next.department + ". Call queued.";
        }

        agent.receiveCall();
        String callId = "C" + String.format("%03d", callCounter++);
        Call call = new Call(callId, next, agent);
        call.startCall();
        activeCalls.addCall(call);
        return "Call " + callId + " assigned: " + next.callerName + " → Agent " + agent.agentName;
    }

    String endCall(String callId) {
        Call call = activeCalls.findCall(callId);
        if (call == null) return "Call not found: " + callId;
        call.finishCall();
        call.agent.endCall();
        activeCalls.removeCall(callId);
        callLog.addLog(call);
        recentEndedCalls.push(call);
        String result = "Call " + callId + " ended. Agent " + call.agent.agentName + " is now available.";
        result += "\n" + assignCall();
        return result;
    }

    Call searchCall(String keyword) {
        ArrayList<Call> allActive = activeCalls.getAllCalls();
        for (Call c : allActive) {
            if (c.callId.equalsIgnoreCase(keyword) ||
                    c.caller.callerName.equalsIgnoreCase(keyword) ||
                    c.agent.agentName.equalsIgnoreCase(keyword)) return c;
        }
        ArrayList<Call> logs = callLog.getAllLogs();
        for (Call c : logs) {
            if (c.callId.equalsIgnoreCase(keyword) ||
                    c.caller.callerName.equalsIgnoreCase(keyword) ||
                    c.agent.agentName.equalsIgnoreCase(keyword)) return c;
        }
        return null;
    }
}

public class CallRoutingSimulator extends JFrame {

    RoutingSystem system = new RoutingSystem();
    int callerCounter = 1;

    JLabel lblTotal, lblActive, lblWaiting, lblAgents;
    DefaultTableModel activeTableModel;
    DefaultListModel<String> waitingListModel;
    DefaultListModel<String> priorityListModel;
    DefaultListModel<String> recentListModel;
    JTextArea logArea;

    public CallRoutingSimulator() {
        setTitle("Voice Call Routing and Queue Handling Simulator");
        setSize(1200, 750);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout(5, 5));
        getContentPane().setBackground(new Color(30, 30, 47));

        add(buildTopPanel(), BorderLayout.NORTH);
        add(buildLeftPanel(), BorderLayout.WEST);
        add(buildCenterPanel(), BorderLayout.CENTER);
        add(buildRightPanel(), BorderLayout.EAST);
        add(buildBottomPanel(), BorderLayout.SOUTH);

        setVisible(true);
        refreshStats();
    }

    JPanel buildTopPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 40, 10));
        panel.setBackground(new Color(20, 20, 35));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        lblTotal = statLabel("Total Calls: 0");
        lblActive = statLabel("Active: 0");
        lblWaiting = statLabel("Waiting: 0");
        lblAgents = statLabel("Available Agents: 6");

        panel.add(lblTotal);
        panel.add(lblActive);
        panel.add(lblWaiting);
        panel.add(lblAgents);
        return panel;
    }

    JLabel statLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setForeground(new Color(100, 220, 255));
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 14));
        return lbl;
    }

    JPanel buildLeftPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(new Color(35, 35, 55));
        panel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(100, 100, 180)),
                "Incoming Call", 0, 0, new Font("Segoe UI", Font.BOLD, 13), Color.WHITE));
        panel.setPreferredSize(new Dimension(220, 0));

        JTextField nameField = styledField();
        JComboBox<String> deptBox = new JComboBox<>(new String[]{"Sales", "Support", "Billing"});
        JComboBox<String> typeBox = new JComboBox<>(new String[]{"Normal", "VIP", "Emergency"});

        styleCombo(deptBox);
        styleCombo(typeBox);

        JButton addBtn = styledButton("Add Call", new Color(50, 180, 100));

        addBtn.addActionListener(e -> {
            String name = nameField.getText().trim();
            if (name.isEmpty()) { log("Enter caller name."); return; }
            String callerId = "CAL" + String.format("%03d", callerCounter++);
            Caller caller = new Caller(callerId, name, typeBox.getSelectedItem().toString(), deptBox.getSelectedItem().toString());
            String result = system.routeCall(caller);
            log(result);
            nameField.setText("");
            refreshAll();
        });

        panel.add(Box.createVerticalStrut(15));
        panel.add(leftLabel("Caller Name:"));
        panel.add(nameField);
        panel.add(Box.createVerticalStrut(8));
        panel.add(leftLabel("Department:"));
        panel.add(deptBox);
        panel.add(Box.createVerticalStrut(8));
        panel.add(leftLabel("Priority:"));
        panel.add(typeBox);
        panel.add(Box.createVerticalStrut(15));
        panel.add(addBtn);
        panel.add(Box.createVerticalStrut(20));

        JButton searchBtn = styledButton("Search Call", new Color(70, 130, 200));
        JTextField searchField = styledField();
        searchBtn.addActionListener(e -> {
            String kw = searchField.getText().trim();
            if (kw.isEmpty()) { log("Enter search keyword."); return; }
            Call result = system.searchCall(kw);
            if (result != null) {
                log("Found: " + result.callId + " | " + result.caller.callerName + " → " + result.agent.agentName + " | " + result.status);
            } else {
                log("No call found for: " + kw);
            }
        });

        panel.add(leftLabel("Search (ID/Name):"));
        panel.add(searchField);
        panel.add(Box.createVerticalStrut(8));
        panel.add(searchBtn);

        return panel;
    }

    JPanel buildCenterPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(new Color(30, 30, 47));

        String[] cols = {"Call ID", "Caller", "Type", "Agent", "Department", "Status"};
        activeTableModel = new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable table = new JTable(activeTableModel);
        table.setBackground(new Color(40, 40, 60));
        table.setForeground(Color.WHITE);
        table.setGridColor(new Color(70, 70, 100));
        table.getTableHeader().setBackground(new Color(50, 50, 80));
        table.getTableHeader().setForeground(new Color(150, 200, 255));
        table.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        table.setRowHeight(26);
        table.setSelectionBackground(new Color(80, 80, 130));

        JPanel topBar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        topBar.setBackground(new Color(30, 30, 47));
        JTextField endField = styledField();
        endField.setPreferredSize(new Dimension(100, 30));
        JButton endBtn = styledButton("End Call", new Color(200, 70, 70));
        endBtn.addActionListener(e -> {
            String cid = endField.getText().trim();
            if (cid.isEmpty()) { log("Enter Call ID to end."); return; }
            String result = system.endCall(cid);
            log(result);
            endField.setText("");
            refreshAll();
        });

        JLabel title = new JLabel("  Active Calls (Doubly Linked List)");
        title.setForeground(new Color(100, 220, 255));
        title.setFont(new Font("Segoe UI", Font.BOLD, 14));
        topBar.add(title);
        topBar.add(new JLabel("  End Call ID: ") {{setForeground(Color.WHITE);}});
        topBar.add(endField);
        topBar.add(endBtn);

        panel.add(topBar, BorderLayout.NORTH);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        return panel;
    }

    JPanel buildRightPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(new Color(35, 35, 55));
        panel.setPreferredSize(new Dimension(200, 0));

        waitingListModel = new DefaultListModel<>();
        JList<String> waitingList = styledList(waitingListModel);

        priorityListModel = new DefaultListModel<>();
        JList<String> priorityList = styledList(priorityListModel);

        JPanel waitPanel = titledPanel("Waiting Queue (Normal)", waitingList);
        JPanel prioPanel = titledPanel("Priority Heap (VIP/Emergency)", priorityList);

        panel.add(waitPanel, BorderLayout.NORTH);
        panel.add(prioPanel, BorderLayout.CENTER);
        return panel;
    }

    JPanel buildBottomPanel() {
        JPanel panel = new JPanel(new GridLayout(1, 2, 5, 0));
        panel.setBackground(new Color(30, 30, 47));
        panel.setPreferredSize(new Dimension(0, 180));

        recentListModel = new DefaultListModel<>();
        JList<String> recentList = styledList(recentListModel);
        panel.add(titledPanel("Recent Ended Calls (Stack)", recentList));

        logArea = new JTextArea();
        logArea.setBackground(new Color(20, 20, 35));
        logArea.setForeground(new Color(150, 255, 150));
        logArea.setFont(new Font("Consolas", Font.PLAIN, 12));
        logArea.setEditable(false);
        JScrollPane logScroll = new JScrollPane(logArea);
        JPanel logPanel = new JPanel(new BorderLayout());
        logPanel.setBackground(new Color(30, 30, 47));
        logPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(100, 100, 180)),
                "System Log", 0, 0, new Font("Segoe UI", Font.BOLD, 13), Color.WHITE));
        logPanel.add(logScroll);
        panel.add(logPanel);

        return panel;
    }

    JPanel titledPanel(String title, JList<?> list) {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(new Color(35, 35, 55));
        p.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(100, 100, 180)),
                title, 0, 0, new Font("Segoe UI", Font.BOLD, 12), Color.WHITE));
        p.add(new JScrollPane(list));
        return p;
    }

    JList<String> styledList(DefaultListModel<String> model) {
        JList<String> list = new JList<>(model);
        list.setBackground(new Color(40, 40, 60));
        list.setForeground(Color.WHITE);
        list.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        list.setSelectionBackground(new Color(80, 80, 130));
        return list;
    }

    JTextField styledField() {
        JTextField f = new JTextField();
        f.setBackground(new Color(50, 50, 75));
        f.setForeground(Color.WHITE);
        f.setCaretColor(Color.WHITE);
        f.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        f.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(100, 100, 180)),
                BorderFactory.createEmptyBorder(3, 6, 3, 6)));
        f.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
        return f;
    }

    void styleCombo(JComboBox<String> box) {
        box.setBackground(new Color(50, 50, 75));
        box.setForeground(Color.WHITE);
        box.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        box.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
    }

    JButton styledButton(String text, Color bg) {
        JButton btn = new JButton(text);
        btn.setBackground(bg);
        btn.setForeground(Color.WHITE);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setAlignmentX(Component.CENTER_ALIGNMENT);
        btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 35));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return btn;
    }

    JLabel leftLabel(String text) {
        JLabel lbl = new JLabel("  " + text);
        lbl.setForeground(new Color(180, 180, 220));
        lbl.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        return lbl;
    }

    void refreshAll() {
        refreshStats();
        refreshActiveTable();
        refreshQueues();
        refreshRecent();
    }

    void refreshStats() {
        int active = system.activeCalls.getAllCalls().size();
        int waiting = system.waitingQueue.size() + system.priorityHeap.size();
        int available = 0;
        for (Agent a : system.agentTree.getAllAgents()) {
            if (a.isAvailable) available++;
        }
        int total = callerCounter - 1;
        lblTotal.setText("Total Calls: " + total);
        lblActive.setText("Active: " + active);
        lblWaiting.setText("Waiting: " + waiting);
        lblAgents.setText("Available Agents: " + available);
    }

    void refreshActiveTable() {
        activeTableModel.setRowCount(0);
        for (Call c : system.activeCalls.getAllCalls()) {
            activeTableModel.addRow(new Object[]{
                    c.callId, c.caller.callerName, c.caller.callType,
                    c.agent.agentName, c.caller.department, c.status
            });
        }
    }

    void refreshQueues() {
        waitingListModel.clear();
        int i = 1;
        for (Caller c : system.waitingQueue) {
            waitingListModel.addElement(i++ + ". " + c.callerName + " (" + c.department + ")");
        }

        priorityListModel.clear();
        ArrayList<Caller> heapCopy = new ArrayList<>(system.priorityHeap);
        heapCopy.sort((a, b) -> a.getPriority() - b.getPriority());
        for (Caller c : heapCopy) {
            priorityListModel.addElement(c.callType + " - " + c.callerName + " (" + c.department + ")");
        }
    }

    void refreshRecent() {
        recentListModel.clear();
        ArrayList<Call> stack = new ArrayList<>(system.recentEndedCalls);
        for (int i = stack.size() - 1; i >= 0; i--) {
            Call c = stack.get(i);
            recentListModel.addElement(c.callId + " | " + c.caller.callerName + " | " + c.agent.agentName);
        }
    }

    void log(String msg) {
        logArea.append("> " + msg + "\n");
        logArea.setCaretPosition(logArea.getDocument().getLength());
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() { public void run() { new CallRoutingSimulator(); } });
    }
}
