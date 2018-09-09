package com.hbgj;

import com.hbgj.entity.PairList;
import com.hbgj.entity.Table;
import com.hbgj.http.util.HttpUtil;
import com.hbgj.http.util.ListProcessData;
import com.hbgj.http.util.TablePartionFiedAndDesc;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.event.EventTarget;
import javafx.event.EventType;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import com.hbgj.App.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.Shadow;
import javafx.scene.image.Image;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.apache.log4j.Logger;
import scala.Tuple2;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import static com.hbgj.App.*;

public class AppController {
    @FXML
   private ComboBox remotedb;
    @FXML
    private ComboBox remotetabs;
    @FXML
    private ComboBox remoteregion;
    @FXML
    private TableView reomtefield;
    @FXML
    private Button  btn_search;
    @FXML
    private Button btn_import;
    @FXML
    private ToggleButton btn_change;
    @FXML
    private TextField input_search;
    @FXML
    private ProgressBar processBar;

    private DropShadow shadow = new DropShadow();

    private static final Logger LOGGER= Logger.getLogger(AppController.class);

    public void changeDb(){
        Object db=remotedb.getValue();
        if(null!=db)current_db=db.toString();
        List<String> objs= null;
        try {
            objs = HttpUtil.getResult(
                    REMOTE+HttpUtil.SHOW_TABLES(),
                    "dbName="+current_db,
                    ListProcessData.getInstance());
        } catch (Exception e) {
            e.printStackTrace();
        }
        CONTEXT.put(REMOTE_TABS,objs);
        if(null!=objs && !objs.isEmpty()){
             current_table=objs.get(0);
        }
        //加载数据
        current_table=App.initCombox(REMOTE_TABS,remotetabs);
        if(objs.isEmpty()){
            current_table=null;
            current_partition=null;
            CONTEXT.put(REMOTE_REGIONS,null);
            //加载数据
            initCombox(REMOTE_REGIONS,remoteregion);
            reomtefield.setItems(FXCollections.observableArrayList());
        }
    }

    public void changeTabs(ActionEvent actionEvent) {
        Object table=remotetabs.getValue();
        if(null!=table)current_table=table.toString();
        List<String> obj= null;
        try {
            obj = HttpUtil.getResult(
                    REMOTE+HttpUtil.SHOW_PARTITIONS(),
                    "dbName="+current_db+"&tableName="+current_table,
                    ListProcessData.getInstance());
        } catch (Exception e) {
            e.printStackTrace();
        }
        Tuple2<String, String> pk_tdesc=TablePartionFiedAndDesc.getPartionFieldAndDesc();
        current_table_desc=pk_tdesc._2;
        if(obj==null){
            LOGGER.info("`"+current_db+"`.`"+current_table+"` 是一个非分区表");
            current_partition=null;
        }else if(obj.isEmpty()){
            LOGGER.info("`"+current_db+"`.`"+current_table+"` 是一个分区表,但表中没有数据");
            current_partition=pk_tdesc._1;
        }
        CONTEXT.put(REMOTE_REGIONS,obj);
        //加载数据
        initCombox(REMOTE_REGIONS,remoteregion);
        initTableView(REMOTE_FIELDS,reomtefield);
    }
    public void click(ActionEvent actionEvent) {
        try {
            EventTarget target=actionEvent.getTarget();
            if(target instanceof  Button) {
                Button button= (Button) target;
                String id=button.getId();
                if("btn_search".equals(id)){
                    String search=input_search.getText();
                    boolean notSpace=checkForNotNull(search);
                    if(!notSpace){
                        infomation("提示","您好,输入框内为空白");
                    }else{
                        search=search.trim();
                        int result=parseContent(search);
                        if(result==-1){
                            infomation("提示","您好,输入框数据不符合规范");
                        }else if(result==0){
                            searchFor(search);
                        }

                    }
                }else if("btn_import".equals(id)){

                    /**
                     * 判断当前环境 如果是REMOTE则 导数据如果不是则提示
                     */
                    //建表
                    // current_db
                    // current_table
                    // 判断当前表是否存在
                    //数据导出
                    // 数据导入
                    // 完毕
                    if(LOCAL.equals(REMOTE)){
                          infomation("警告","当前环境是本地HIVE数据库不能导出数据");
                     }else try {
                        processBar.setProgress(0.0d);
                        List<String> dbs = HttpUtil.getResult(
                                LOCAL + HttpUtil.SHOW_DATABASES(), "",
                                ListProcessData.getInstance());

                        if (!dbs.contains(current_db)) {
                            //建库 %2
                            boolean create_db = HttpUtil.execUpdate(
                                    LOCAL + HttpUtil.EXECUTE_UPDATE(),
                                    "dbName=default&sql=create database " + current_db
                            );
                            if (create_db) {
                                infomation("提示",
                                        "创建数据库" + current_db + "失败");
                                return;
                            }
                        }
                        processBar.setProgress(0.02d);
                        List<String> tabs = HttpUtil.getResult(
                                LOCAL + HttpUtil.SHOW_TABLES(), "dbName=" + current_db,
                                ListProcessData.getInstance());
                        if (!tabs.contains(current_table)) {
                            // 建表 %3
                            // current_db current_table
                            ObservableList<Table> items = reomtefield.getItems();

                            StringBuffer sb = new StringBuffer("CREATE TABLE `");
                            sb.append(current_db).append(".")
                                    .append(current_table)
                                    .append("`(");
                            for (Table t : items) {
                                String fdesc = t.getFdesc();
                                String ftype = t.getFtype();
                                String fname = t.getFname();

                                if (fname.equals(current_partition)) {
                                    sb=sb.delete(sb.length()-1,sb.length());
                                    sb.append(" )  PARTITIONED BY (  `").append(t.getFname()).append("` ")
                                            .append(t.getFtype());
                                    if(fdesc!=null || !fdesc.isEmpty()) {
                                        sb.append(" comment '")
                                                .append(fdesc).append("' ) ROW FORMAT DELIMITED FIELDS TERMINATED BY '|' STORED AS TEXTFILE ");

                                    }else{
                                        sb.append(" ) ROW FORMAT DELIMITED FIELDS TERMINATED BY '|' STORED AS TEXTFILE ");

                                    }
                                } else {
                                    if("#type".equals(ftype) ){
                                         if(current_partition==null){
                                             sb=sb.delete(sb.length()-1,sb.length());
                                             if(fdesc!=null && !fdesc.isEmpty()) {
                                                 sb.append(" ) comment '").append(fdesc).append("'  ");
                                             }else{
                                                 sb.append(" ) ");
                                             }

                                             sb.append("  ROW FORMAT DELIMITED FIELDS TERMINATED BY '|' STORED AS TEXTFILE ");

                                         }
                                    }else{
                                        sb.append(" `").append(t.getFname()).append("` ")
                                                .append(t.getFtype());
                                        if(fdesc!=null && !fdesc.isEmpty()) {
                                            sb.append(" comment '").append(fdesc).append("',");
                                        }else{
                                            sb.append(",");
                                        }
                                    }
                                }


                            }

                            boolean create_tab =HttpUtil.execUpdate(
                                    LOCAL+HttpUtil.EXECUTE_UPDATE(),
                                    "dbName="+current_db+"&sql="+sb.toString()
                            );
                            if (!create_tab) {
                                infomation("提示",
                                        "创建表 `" + current_db+"`.`"+current_table + "` 失败");
                                return;
                            }
                        }
                        processBar.setProgress(0.05d);
                        // 导出数据 每分钟 增加%5  知道增加到50%

                        processBar.setProgress(0.5d);
                        //导入数据

                        processBar.setProgress(1.0d);

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }else if(target instanceof ToggleButton){
                ToggleButton button= (ToggleButton) target;

               String id= button.getId();

               if("btn_change".equals(id)){
                   if(REMOTE.equals(LOCAL))REMOTE="";
                   else REMOTE=LOCAL;
                   //加载数据
                   List<String> obj= null;
                   try {
                       obj = HttpUtil.getResult(
                               REMOTE+HttpUtil.SHOW_DATABASES(), "",
                               ListProcessData.getInstance()
                       );
                   } catch (Exception e) {
                       e.printStackTrace();
                   }
                   CONTEXT.put(REMOTE_DBS,obj);
                   if(obj!=null && !obj.isEmpty()){
                       current_db=obj.get(0);
                   }else{
                       current_db="default";
                   }
                   current_db=initCombox(REMOTE_DBS,remotedb);

               }


            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void searchFor(String tableName) {
        List<String> tabs= null;
        try {
            tabs = HttpUtil.getResult(
                    REMOTE+HttpUtil.SHOW_TABLES(),
                    "dbName="+current_db,
                    ListProcessData.getInstance());
        } catch (Exception e) {
            e.printStackTrace();
        }
        PairList<Float,String> pairList=new PairList<Float, String>();
        if(null!=tabs && !tabs.isEmpty()){
            int index=1;
            for (String tname: tabs) {
                 float degree;
                 if(tname.startsWith(tableName))degree=3.0f;
                 else if(tname.endsWith(tableName))degree=2.0f;
                 else if(tname.contains(tableName))degree=1.0f;
                 else degree=Similar.getSimilarityRatio(tableName,tname);

                 pairList.add(degree+(index++)*0.00001f,tname);
            }

            List<String> tnames=pairList.sort(new Comparator<Float>() {
                @Override
                public int compare(Float o1, Float o2) {
                    float rs=o2-o1;
                    return rs>0?1:-1;
                }
            });
            current_table=tnames.get(0);

             CONTEXT.put(REMOTE_TABS,tnames);
            //加载数据
            current_table=App.initCombox(REMOTE_TABS,remotetabs);

        }else{
            try {
                infomation("提示","查询的结果为空");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    /**
     *
     * @param search
     * @return -1 代表解析失败  0  解析成功
     */
    private int parseContent(String search) {
        String table="([0-9a-zA-Z]|[_]|[-])+";
        if(search.matches(table)){
            return 0;
        }else {
            return -1;
        }
    }

    private boolean checkForNotNull(String search) {
        if(null==search || search.isEmpty() || search.trim().isEmpty()) return false;

        return true;
    }

    public void mouseEnter(MouseEvent mouseEvent) {
        EventTarget target= mouseEvent.getTarget();

        if(target instanceof  Button){
            Button t1= (Button) target;
            t1.setEffect(shadow);
        }else if(target instanceof  ToggleButton){
            ToggleButton t1= (ToggleButton) target;
            t1.setEffect(shadow);
        }
    }

    public void mouseExit(MouseEvent mouseEvent) {
        EventTarget target= mouseEvent.getTarget();
        if(target instanceof  Button){
            Button t1= (Button) target;
            t1.setEffect(null);
        }else if(target instanceof  ToggleButton){
            ToggleButton t1= (ToggleButton) target;
            t1.setEffect(null);
        }
    }


    /**
     * 弹出一个通用的确定对话框
     * @param p_header 对话框的信息标题
     * @param p_message 对话框的信息
     * @return 用户点击了是或否
     */
    public boolean f_alert_confirmDialog(String p_header,String p_message){
//        按钮部分可以使用预设的也可以像这样自己 new 一个
        Alert _alert = new Alert(Alert.AlertType.CONFIRMATION,p_message,new ButtonType("取消", ButtonBar.ButtonData.NO),
                new ButtonType("确定", ButtonBar.ButtonData.YES));
//        设置窗口的标题
        _alert.setTitle("确认");
        _alert.setHeaderText(p_header);
//        设置对话框的 icon 图标，参数是主窗口的 stage
        _alert.initOwner(App.mainWindow);
//        showAndWait() 将在对话框消失以前不会执行之后的代码
        Optional<ButtonType> _buttonType = _alert.showAndWait();
//        根据点击结果返回
        if(_buttonType.get().getButtonData().equals(ButtonBar.ButtonData.YES)){
            return true;
        }
        else {
            return false;
        }
    }

    //    弹出一个信息对话框
    public void f_alert_informationDialog(String p_header, String p_message){
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("信息");
        alert.setHeaderText(p_header);
        alert.setContentText(p_message);

        //alert.initOwner(App.mainWindow);
        alert.show();
    }


    public void infomation(String title,String  message) throws IOException {
        AnchorPane root = FXMLLoader
                .load(getClass().getResource("../../information.fxml"));
        Stage stage = new Stage();
        stage.setTitle(title);
        stage.getIcons().add(new Image(
                getClass().getResourceAsStream("../../space.png")
        ));
        stage.setResizable(false);
        stage.setScene(new Scene(root, 300, 90));
        stage.initModality(Modality.APPLICATION_MODAL);

        root.getChildren().filtered(node -> {
            String id=node.getId();
            if("message".equals(id)){
                Label label= (Label) node;
                label.setText(message);
            }else if("info_button".equals(id)){
                Button button= (Button) node;
                button.setOnMouseEntered(new EventHandler<MouseEvent>() {
                    @Override
                    public void handle(MouseEvent event) {
                        button.setEffect(shadow);
                    }
                });
                button.setOnMouseExited(new EventHandler<MouseEvent>() {
                    @Override
                    public void handle(MouseEvent event) {
                        button.setEffect(null);
                    }
                });

                button.setOnMouseClicked(new EventHandler<MouseEvent>() {
                    @Override
                    public void handle(MouseEvent event) {
                        stage.hide();
                    }
                });
            }

            return false;
        });

        stage.show();
    }
}
