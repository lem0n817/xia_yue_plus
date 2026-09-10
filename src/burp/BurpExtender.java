package burp;

import burp.IBurpExtender;
import burp.IBurpExtenderCallbacks;
import burp.IExtensionHelpers;
import burp.IHttpListener;
import burp.IHttpRequestResponse;
import burp.IHttpRequestResponsePersisted;
import burp.IHttpService;
import burp.IMessageEditor;
import burp.IMessageEditorController;
import burp.IParameter;
import burp.IRequestInfo;
import burp.IScanIssue;
import burp.IScannerCheck;
import burp.IScannerInsertionPoint;
import burp.ITab;
import java.awt.Color;
import java.awt.Component;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.PrintWriter;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableModel;
import plus.rule.RuleEngine;
import plus.rule.RuleStore;
import plus.ui.ColorScheme;

/**
 * xia Yue Plus V1.3 —— 基于 xia Yue V1.2（作者：算命编子）修改。
 * 新增：低权限/未授权响应的敏感数据差集检测 + 整行染色（规则联动 HaE Rules.yml）。
 */
public class BurpExtender
extends AbstractTableModel
implements IBurpExtender,
ITab,
IHttpListener,
IScannerCheck,
IMessageEditorController {
    private IBurpExtenderCallbacks callbacks;
    private IExtensionHelpers helpers;
    private JSplitPane splitPane;
    private IMessageEditor requestViewer;
    private IMessageEditor responseViewer;
    private IMessageEditor requestViewer_1;
    private IMessageEditor responseViewer_1;
    private IMessageEditor requestViewer_2;
    private IMessageEditor responseViewer_2;
    private final List<LogEntry> log = new ArrayList<LogEntry>();
    private IHttpRequestResponse currentlyDisplayedItem;
    private IHttpRequestResponse currentlyDisplayedItem_1;
    private IHttpRequestResponse currentlyDisplayedItem_2;
    private final List<Request_md5> log4_md5 = new ArrayList<Request_md5>();
    public PrintWriter stdout;
    JTabbedPane tabs;
    int switchs = 0;
    int conut = 0;
    int original_data_len;
    String temp_data;
    int select_row = 0;
    Table logTable;
    String white_URL = "";
    int white_switchs = 0;
    String data_1 = "";
    String data_2 = "";
    String universal_cookie = "";
    private RuleStore ruleStore;
    private RuleEngine ruleEngine;
    private JLabel ruleStatusLabel;

    @Override
    public void registerExtenderCallbacks(final IBurpExtenderCallbacks callbacks) {
        this.stdout = new PrintWriter(callbacks.getStdout(), true);
        this.stdout.println("hello xia Yue!");
        this.stdout.println("\u4f60\u597d \u6b22\u8fce\u4f7f\u7528 \u778e\u8d8a!");
        this.stdout.println("version:1.3-plus (rule colorized)");
        this.callbacks = callbacks;
        this.helpers = callbacks.getHelpers();
        callbacks.setExtensionName("xia Yue Plus V1.3");

        this.ruleStore = new RuleStore();
        this.ruleEngine = new RuleEngine(this.ruleStore);
        this.reloadRules();

        SwingUtilities.invokeLater(new Runnable(){

            @Override
            public void run() {
                BurpExtender.this.splitPane = new JSplitPane(1);
                JSplitPane splitPanes = new JSplitPane(0);
                JSplitPane splitPanes_2 = new JSplitPane(0);
                BurpExtender.this.logTable = new Table(BurpExtender.this);
                BurpExtender.this.logTable.getColumnModel().getColumn(0).setPreferredWidth(10);
                BurpExtender.this.logTable.getColumnModel().getColumn(1).setPreferredWidth(50);
                BurpExtender.this.logTable.getColumnModel().getColumn(2).setPreferredWidth(300);
                JScrollPane scrollPane = new JScrollPane(BurpExtender.this.logTable);
                JPanel jp = new JPanel();
                jp.setLayout(new GridLayout(1, 1));
                jp.add(scrollPane);
                JPanel jps = new JPanel();
                jps.setLayout(new GridLayout(12, 1));
                JLabel jls = new JLabel("\u63d2\u4ef6\u540d\uff1a\u778e\u8d8a author\uff1a\u7b97\u547d\u7e16\u5b50\u3001lemoni");
                JLabel jls_1 = new JLabel("\u5410\u53f8:www.t00ls.com");
                JLabel jls_2 = new JLabel("\u7248\u672c\uff1axia Yue Plus V1.3");
                JLabel jls_3 = new JLabel("\u611f\u8c22\u540d\u5355\uff1aMoonlit");
                final JCheckBox chkbox1 = new JCheckBox("\u542f\u52a8\u63d2\u4ef6");
                final JCheckBox chkbox2 = new JCheckBox("\u542f\u52a8\u4e07\u80fdcookie");
                JLabel jls_5 = new JLabel("\u5982\u679c\u9700\u8981\u591a\u4e2a\u57df\u540d\u52a0\u767d\u8bf7\u7528,\u9694\u5f00");
                final JTextField textField = new JTextField("\u586b\u5199\u767d\u540d\u5355\u57df\u540d");
                JButton btn1 = new JButton("\u6e05\u7a7a\u5217\u8868");
                final JButton btn3 = new JButton("\u542f\u52a8\u767d\u540d\u5355");
                final JButton btnReload = new JButton("\u91cd\u8f7d\u89c4\u5219");
                BurpExtender.this.ruleStatusLabel = new JLabel(BurpExtender.this.ruleStatusText());
                JPanel jps_2 = new JPanel();
                JLabel jps_2_jls_1 = new JLabel("\u8d8a\u6743\uff1a\u586b\u5199\u4f4e\u6743\u9650\u8ba4\u8bc1\u4fe1\u606f,\u5c06\u4f1a\u66ff\u6362\u6216\u65b0\u589e");
                final JTextArea jta = new JTextArea("Cookie: JSESSIONID=test;UUID=1; userid=admin\nAuthorization: Bearer test", 5, 30);
                JScrollPane jsp = new JScrollPane(jta);
                JLabel jps_2_jls_2 = new JLabel("\u672a\u6388\u6743\uff1a\u5c06\u79fb\u9664\u4e0b\u5217\u5934\u90e8\u8ba4\u8bc1\u4fe1\u606f,\u533a\u5206\u5927\u5c0f\u5199");
                final JTextArea jta_1 = new JTextArea("Cookie\nAuthorization\nToken", 5, 30);
                JScrollPane jsp_1 = new JScrollPane(jta_1);
                jps_2.add(jps_2_jls_1);
                jps_2.add(jsp);
                jps_2.add(jps_2_jls_2);
                jps_2.add(jsp_1);
                jps_2.setLayout(new GridLayout(5, 1, 0, 0));
                chkbox1.addItemListener(new ItemListener(){

                    @Override
                    public void itemStateChanged(ItemEvent e) {
                        if (chkbox1.isSelected()) {
                            BurpExtender.this.switchs = 1;
                            BurpExtender.this.data_1 = jta.getText();
                            BurpExtender.this.data_2 = jta_1.getText();
                            jta.setForeground(Color.BLACK);
                            jta.setBackground(Color.LIGHT_GRAY);
                            jta.setEditable(false);
                            jta_1.setForeground(Color.BLACK);
                            jta_1.setBackground(Color.LIGHT_GRAY);
                            jta_1.setEditable(false);
                        } else {
                            BurpExtender.this.switchs = 0;
                            jta.setForeground(Color.BLACK);
                            jta.setBackground(Color.WHITE);
                            jta.setEditable(true);
                            jta_1.setForeground(Color.BLACK);
                            jta_1.setBackground(Color.WHITE);
                            jta_1.setEditable(true);
                        }
                    }
                });
                chkbox2.addItemListener(new ItemListener(){

                    @Override
                    public void itemStateChanged(ItemEvent e) {
                        BurpExtender.this.universal_cookie = chkbox2.isSelected() ? "" : "";
                    }
                });
                btn1.addActionListener(new ActionListener(){

                    @Override
                    public void actionPerformed(ActionEvent e) {
                        BurpExtender.this.log.clear();
                        BurpExtender.this.conut = 0;
                        BurpExtender.this.log4_md5.clear();
                        BurpExtender.this.fireTableRowsInserted(BurpExtender.this.log.size(), BurpExtender.this.log.size());
                    }
                });
                btn3.addActionListener(new ActionListener(){

                    @Override
                    public void actionPerformed(ActionEvent e) {
                        if (btn3.getText().equals("\u542f\u52a8\u767d\u540d\u5355")) {
                            btn3.setText("\u5173\u95ed\u767d\u540d\u5355");
                            BurpExtender.this.white_URL = textField.getText();
                            BurpExtender.this.white_switchs = 1;
                            textField.setEditable(false);
                            textField.setForeground(Color.GRAY);
                        } else {
                            btn3.setText("\u542f\u52a8\u767d\u540d\u5355");
                            BurpExtender.this.white_switchs = 0;
                            textField.setEditable(true);
                            textField.setForeground(Color.BLACK);
                        }
                    }
                });
                btnReload.addActionListener(new ActionListener(){

                    @Override
                    public void actionPerformed(ActionEvent e) {
                        BurpExtender.this.reloadRules();
                        BurpExtender.this.ruleStatusLabel.setText(BurpExtender.this.ruleStatusText());
                    }
                });
                jps.add(jls);
                jps.add(jls_1);
                jps.add(jls_2);
                jps.add(jls_3);
                jps.add(chkbox1);
                jps.add(btn1);
                jps.add(jls_5);
                jps.add(textField);
                jps.add(btn3);
                jps.add(btnReload);
                jps.add(BurpExtender.this.ruleStatusLabel);
                BurpExtender.this.tabs = new JTabbedPane();
                BurpExtender.this.requestViewer = callbacks.createMessageEditor(BurpExtender.this, false);
                BurpExtender.this.responseViewer = callbacks.createMessageEditor(BurpExtender.this, false);
                BurpExtender.this.requestViewer_1 = callbacks.createMessageEditor(BurpExtender.this, false);
                BurpExtender.this.responseViewer_1 = callbacks.createMessageEditor(BurpExtender.this, false);
                BurpExtender.this.requestViewer_2 = callbacks.createMessageEditor(BurpExtender.this, false);
                BurpExtender.this.responseViewer_2 = callbacks.createMessageEditor(BurpExtender.this, false);
                JSplitPane y_jp = new JSplitPane(1);
                y_jp.setDividerLocation(500);
                y_jp.setLeftComponent(BurpExtender.this.requestViewer.getComponent());
                y_jp.setRightComponent(BurpExtender.this.responseViewer.getComponent());
                JSplitPane d_jp = new JSplitPane(1);
                d_jp.setDividerLocation(500);
                d_jp.setLeftComponent(BurpExtender.this.requestViewer_1.getComponent());
                d_jp.setRightComponent(BurpExtender.this.responseViewer_1.getComponent());
                JSplitPane w_jp = new JSplitPane(1);
                w_jp.setDividerLocation(500);
                w_jp.setLeftComponent(BurpExtender.this.requestViewer_2.getComponent());
                w_jp.setRightComponent(BurpExtender.this.responseViewer_2.getComponent());
                BurpExtender.this.tabs.addTab("\u539f\u59cb\u6570\u636e\u5305", y_jp);
                BurpExtender.this.tabs.addTab("\u4f4e\u6743\u9650\u6570\u636e\u5305", d_jp);
                BurpExtender.this.tabs.addTab("\u672a\u6388\u6743\u6570\u636e\u5305", w_jp);
                splitPanes_2.setLeftComponent(jps);
                splitPanes_2.setRightComponent(jps_2);
                splitPanes.setLeftComponent(jp);
                splitPanes.setRightComponent(BurpExtender.this.tabs);
                BurpExtender.this.splitPane.setLeftComponent(splitPanes);
                BurpExtender.this.splitPane.setRightComponent(splitPanes_2);
                BurpExtender.this.splitPane.setDividerLocation(1000);
                callbacks.customizeUiComponent(BurpExtender.this.splitPane);
                callbacks.customizeUiComponent(BurpExtender.this.logTable);
                callbacks.customizeUiComponent(scrollPane);
                callbacks.customizeUiComponent(jps);
                callbacks.customizeUiComponent(jp);
                callbacks.customizeUiComponent(BurpExtender.this.tabs);
                callbacks.addSuiteTab(BurpExtender.this);
                callbacks.registerHttpListener(BurpExtender.this);
                callbacks.registerScannerCheck(BurpExtender.this);
            }
        });
    }

    private void reloadRules() {
        try {
            this.ruleStore.reload();
            RuleStore.Stats s = this.ruleStore.getStats();
            this.stdout.println("[xia_yue_plus] 规则来源: " + this.ruleStore.getSourceType()
                    + " (" + this.ruleStore.getSourceFilePath() + ")");
            this.stdout.println("[xia_yue_plus] 染色规则 " + s.loaded + " 条 | 指纹排除 " + s.fingerprint
                    + " | 作用域排除 " + s.scope + " | 无色排除 " + s.noneColor
                    + " | 未启用 " + s.notLoaded + " | 正则错误 " + s.error);
        } catch (Exception e) {
            this.stdout.println("[xia_yue_plus] 规则加载失败: " + e.getMessage());
        }
    }

    private String ruleStatusText() {
        if (this.ruleStore == null || !this.ruleStore.isLoaded()) {
            return "染色规则: 未加载";
        }
        return "来源:" + this.ruleStore.getSourceType() + " · " + this.ruleStore.getStats().loaded + "条染色规则";
    }

    @Override
    public String getTabCaption() {
        return "Yue Plus";
    }

    @Override
    public Component getUiComponent() {
        return this.splitPane;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public void processHttpMessage(final int toolFlag, boolean messageIsRequest, final IHttpRequestResponse messageInfo) {
        if (this.switchs == 1 && toolFlag == 4 && !messageIsRequest) {
            List<LogEntry> list = this.log;
            synchronized (list) {
                Thread thread = new Thread(new Runnable(){

                    @Override
                    public void run() {
                        try {
                            BurpExtender.this.checkVul(messageInfo, toolFlag);
                        }
                        catch (Exception ex) {
                            ex.printStackTrace();
                            BurpExtender.this.stdout.println(ex);
                        }
                    }
                });
                thread.start();
            }
        }
    }

    @Override
    public List<IScanIssue> doPassiveScan(IHttpRequestResponse baseRequestResponse) {
        return null;
    }

    private void checkVul(IHttpRequestResponse baseRequestResponse, int toolFlag) {
        String url = this.helpers.analyzeRequest(baseRequestResponse).getUrl().toString();
        byte[] originalResponse = baseRequestResponse.getResponse();
        int original_len = originalResponse == null ? 0 : originalResponse.length - this.helpers.analyzeResponse(originalResponse).getBodyOffset();
        String urlNoQuery = url.split("\\?")[0];
        String[] white_URL_list = this.white_URL.split(",");
        if (this.white_switchs == 1) {
            boolean white_swith = false;
            for (String white : white_URL_list) {
                if (white.isEmpty() || !urlNoQuery.contains(white)) continue;
                this.stdout.println("白名单URL！" + urlNoQuery);
                white_swith = true;
            }
            if (!white_swith) {
                this.stdout.println("不是白名单URL！" + urlNoQuery);
                return;
            }
        }
        if (toolFlag == 4 || toolFlag == 64) {
            String[] static_file = new String[]{"jpg", "png", "gif", "css", "js", "pdf", "mp3", "mp4", "avi", "map", "svg", "ico", "woff", "woff2", "ttf"};
            String ext = urlNoQuery.contains(".") ? urlNoQuery.substring(urlNoQuery.lastIndexOf('.') + 1) : "";
            for (String s : static_file) {
                if (!ext.equals(s)) continue;
                this.stdout.println("当前url为静态文件：" + urlNoQuery + "\n");
                return;
            }
        }
        StringBuilder dedupKey = new StringBuilder(urlNoQuery);
        List<IParameter> paraLists = this.helpers.analyzeRequest(baseRequestResponse).getParameters();
        for (IParameter para : paraLists) {
            dedupKey.append('+').append(para.getName());
        }
        dedupKey.append('+').append(this.helpers.analyzeRequest(baseRequestResponse).getMethod());
        this.stdout.println("\nMD5(\"" + dedupKey + "\")");
        String md5 = MD5(dedupKey.toString());
        this.stdout.println(md5);
        for (Request_md5 r : this.log4_md5) {
            if (!r.md5_data.equals(md5)) continue;
            return;
        }
        this.log4_md5.add(new Request_md5(md5));
        IRequestInfo analyIRequestInfo = this.helpers.analyzeRequest(baseRequestResponse);
        IHttpService iHttpService = baseRequestResponse.getHttpService();
        String request = this.helpers.bytesToString(baseRequestResponse.getRequest());
        int bodyOffset = analyIRequestInfo.getBodyOffset();
        byte[] body = request.substring(bodyOffset).getBytes();
        List<String> headers_y = analyIRequestInfo.getHeaders();
        String[] data_1_list = this.data_1.split("\n");
        for (int i = 0; i < headers_y.size(); ++i) {
            String head_key = headers_y.get(i).split(":")[0];
            for (String line : data_1_list) {
                if (!head_key.equals(line.split(":")[0])) continue;
                headers_y.remove(i);
                break;
            }
        }
        for (String line : data_1_list) {
            if (line.trim().isEmpty()) continue;
            headers_y.add(headers_y.size() / 2, line);
        }
        stripAcceptEncoding(headers_y);
        byte[] newRequest_y = this.helpers.buildHttpMessage(headers_y, body);
        IHttpRequestResponse requestResponse_y = this.callbacks.makeHttpRequest(iHttpService, newRequest_y);
        byte[] lowResp = requestResponse_y == null ? null : requestResponse_y.getResponse();
        int low_len = lowResp == null ? 0 : lowResp.length - this.helpers.analyzeResponse(lowResp).getBodyOffset();
        String low_len_data = original_len == 0 ? Integer.toString(low_len) : (original_len == low_len ? Integer.toString(low_len) + "  \u2714" : Integer.toString(low_len) + "  ==> " + Integer.toString(original_len - low_len));
        List<String> headers_w = analyIRequestInfo.getHeaders();
        String[] data_2_list = this.data_2.split("\n");
        for (int i = 0; i < headers_w.size(); ++i) {
            String head_key = headers_w.get(i).split(":")[0];
            for (String line : data_2_list) {
                if (!head_key.equals(line)) continue;
                headers_w.remove(i);
                break;
            }
        }
        if (this.universal_cookie.length() != 0) {
            String[] universal_cookies = this.universal_cookie.split("\n");
            headers_w.add(headers_w.size() / 2, universal_cookies[0]);
            headers_w.add(headers_w.size() / 2, universal_cookies[1]);
        }
        stripAcceptEncoding(headers_w);
        byte[] newRequest_w = this.helpers.buildHttpMessage(headers_w, body);
        IHttpRequestResponse requestResponse_w = this.callbacks.makeHttpRequest(iHttpService, newRequest_w);
        byte[] unauthResp = requestResponse_w == null ? null : requestResponse_w.getResponse();
        int Unauthorized_len = unauthResp == null ? 0 : unauthResp.length - this.helpers.analyzeResponse(unauthResp).getBodyOffset();
        String original_len_data = original_len == 0 ? Integer.toString(Unauthorized_len) : (original_len == Unauthorized_len ? Integer.toString(Unauthorized_len) + "  \u2714" : Integer.toString(Unauthorized_len) + "  ==> " + Integer.toString(original_len - Unauthorized_len));

        // ---- 敏感数据检测与行染色：低权限/未授权命中即染色 ----
        String matchedColor = null;
        if (this.ruleEngine.hasRules()) {
            try {
                Set<String> lowSet = this.ruleEngine.extractResponse(lowResp);
                Set<String> unauthSet = this.ruleEngine.extractResponse(unauthResp);
                RuleEngine.Match match = this.ruleEngine.hit(lowSet, unauthSet);
                if (match != null) {
                    matchedColor = match.color;
                    // 透传到 Burp 原生高亮（Proxy 历史 / 站点地图可见）
                    try {
                        baseRequestResponse.setHighlight(matchedColor);
                    }
                    catch (Exception ignore) {
                        // empty catch block
                    }
                }
            }
            catch (Exception ex) {
                this.stdout.println("[xia_yue_plus] 染色分析异常: " + ex);
            }
        }

        ++this.conut;
        int id = this.conut;
        this.log.add(new LogEntry(id, analyIRequestInfo.getMethod(), this.callbacks.saveBuffersToTempFiles(baseRequestResponse), this.callbacks.saveBuffersToTempFiles(requestResponse_y), this.callbacks.saveBuffersToTempFiles(requestResponse_w), url, original_len, low_len_data, original_len_data, matchedColor));
        this.fireTableDataChanged();
        this.logTable.setRowSelectionInterval(this.select_row, this.select_row);
    }

    /** 移除 Accept-Encoding，避免低权限/未授权请求收到压缩响应导致规则匹配失败（Proxy 会自动解压，这里不会） */
    private static void stripAcceptEncoding(List<String> headers) {
        for (int i = headers.size() - 1; i >= 0; --i) {
            if (headers.get(i).toLowerCase().startsWith("accept-encoding:")) {
                headers.remove(i);
            }
        }
    }

    @Override
    public List<IScanIssue> doActiveScan(IHttpRequestResponse baseRequestResponse, IScannerInsertionPoint insertionPoint) {
        return null;
    }

    @Override
    public int consolidateDuplicateIssues(IScanIssue existingIssue, IScanIssue newIssue) {
        if (existingIssue.getIssueName().equals(newIssue.getIssueName())) {
            return -1;
        }
        return 0;
    }

    @Override
    public int getRowCount() {
        return this.log.size();
    }

    @Override
    public int getColumnCount() {
        return 6;
    }

    @Override
    public String getColumnName(int columnIndex) {
        switch (columnIndex) {
            case 0: {
                return "#";
            }
            case 1: {
                return "类型";
            }
            case 2: {
                return "URL";
            }
            case 3: {
                return "原始包长度";
            }
            case 4: {
                return "低权限包长度";
            }
            case 5: {
                return "未授权包长度";
            }
        }
        return "";
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        return String.class;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        LogEntry logEntry = this.log.get(rowIndex);
        switch (columnIndex) {
            case 0: {
                return logEntry.id;
            }
            case 1: {
                return logEntry.Method;
            }
            case 2: {
                return logEntry.url;
            }
            case 3: {
                return logEntry.original_len;
            }
            case 4: {
                return logEntry.low_len;
            }
            case 5: {
                return logEntry.Unauthorized_len;
            }
        }
        return "";
    }

    @Override
    public byte[] getRequest() {
        return this.currentlyDisplayedItem.getRequest();
    }

    @Override
    public byte[] getResponse() {
        return this.currentlyDisplayedItem.getResponse();
    }

    @Override
    public IHttpService getHttpService() {
        return this.currentlyDisplayedItem.getHttpService();
    }

    public static String MD5(String key) {
        char[] hexDigits = new char[]{'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};
        try {
            byte[] btInput = key.getBytes();
            MessageDigest mdInst = MessageDigest.getInstance("MD5");
            mdInst.update(btInput);
            byte[] md = mdInst.digest();
            int j = md.length;
            char[] str = new char[j * 2];
            int k = 0;
            for (int i = 0; i < j; ++i) {
                byte byte0 = md[i];
                str[k++] = hexDigits[byte0 >>> 4 & 0xF];
                str[k++] = hexDigits[byte0 & 0xF];
            }
            return new String(str);
        }
        catch (Exception e) {
            return null;
        }
    }

    private static class Request_md5 {
        final String md5_data;

        Request_md5(String md5_data) {
            this.md5_data = md5_data;
        }
    }

    private static class LogEntry {
        final int id;
        final String Method;
        final IHttpRequestResponsePersisted requestResponse;
        final IHttpRequestResponsePersisted requestResponse_1;
        final IHttpRequestResponsePersisted requestResponse_2;
        final String url;
        final int original_len;
        final String low_len;
        final String Unauthorized_len;
        final String matchedColor;

        LogEntry(int id, String Method, IHttpRequestResponsePersisted requestResponse, IHttpRequestResponsePersisted requestResponse_1, IHttpRequestResponsePersisted requestResponse_2, String url, int original_len, String low_len, String Unauthorized_len, String matchedColor) {
            this.id = id;
            this.Method = Method;
            this.requestResponse = requestResponse;
            this.requestResponse_1 = requestResponse_1;
            this.requestResponse_2 = requestResponse_2;
            this.url = url;
            this.original_len = original_len;
            this.low_len = low_len;
            this.Unauthorized_len = Unauthorized_len;
            this.matchedColor = matchedColor;
        }
    }

    private class Table
    extends JTable {
        public Table(TableModel tableModel) {
            super(tableModel);
        }

        @Override
        public void changeSelection(int row, int col, boolean toggle, boolean extend) {
            LogEntry logEntry = BurpExtender.this.log.get(row);
            BurpExtender.this.select_row = row;
            if (col == 4) {
                BurpExtender.this.tabs.setSelectedIndex(1);
            } else if (col == 5) {
                BurpExtender.this.tabs.setSelectedIndex(2);
            } else if (col == 3) {
                BurpExtender.this.tabs.setSelectedIndex(0);
            }
            BurpExtender.this.requestViewer.setMessage(logEntry.requestResponse.getRequest(), true);
            BurpExtender.this.responseViewer.setMessage(logEntry.requestResponse.getResponse(), false);
            BurpExtender.this.currentlyDisplayedItem = logEntry.requestResponse;
            BurpExtender.this.requestViewer_1.setMessage(logEntry.requestResponse_1.getRequest(), true);
            BurpExtender.this.responseViewer_1.setMessage(logEntry.requestResponse_1.getResponse(), false);
            BurpExtender.this.currentlyDisplayedItem_1 = logEntry.requestResponse_1;
            BurpExtender.this.requestViewer_2.setMessage(logEntry.requestResponse_2.getRequest(), true);
            BurpExtender.this.responseViewer_2.setMessage(logEntry.requestResponse_2.getResponse(), false);
            BurpExtender.this.currentlyDisplayedItem_2 = logEntry.requestResponse_2;
            super.changeSelection(row, col, toggle, extend);
        }

        @Override
        public Component prepareRenderer(TableCellRenderer renderer, int row, int column) {
            Component c = super.prepareRenderer(renderer, row, column);
            try {
                List<LogEntry> entries = BurpExtender.this.log;
                synchronized (entries) {
                    if (row >= 0 && row < entries.size()) {
                        LogEntry entry = entries.get(row);
                        // 选中行完全交给 Burp 默认选择色（不做遮罩干预）；仅未选中的染色行上规则色。
                        // 每行都显式复位背景（基色取表格标准底色而非渲染器缓存值），避免多次点击/多选串色。
                        Color bg;
                        if (this.isRowSelected(row)) {
                            bg = this.getSelectionBackground();
                        } else if (entry.matchedColor != null) {
                            bg = ColorScheme.blend(this.getBackground(), ColorScheme.of(entry.matchedColor), 0.45f);
                        } else {
                            bg = this.getBackground();
                        }
                        c.setBackground(bg);
                    }
                }
            }
            catch (Exception ignore) {
                // empty catch block
            }
            return c;
        }
    }
}
