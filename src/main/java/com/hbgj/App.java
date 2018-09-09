package com.hbgj;

import com.hbgj.code.CodeUtil;
import com.hbgj.entity.Table;
import com.hbgj.http.util.HttpUtil;
import com.hbgj.http.util.ListProcessData;
import com.hbgj.http.util.TablePartionFiedAndDesc;
import com.hbgj.http.util.TableProcessData;
import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.image.Image;
import javafx.scene.layout.Pane;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import javafx.util.Callback;
import org.apache.log4j.Logger;
import scala.Tuple2;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Comparator;
import java.util.List;
import java.util.Properties;
/**
 * Hello world!
 */
public class App extends Application
{
   private static String remote_hostname=null;
   private static String remote_username=null;
   private static String remote_password=null;
   private static String local_hostname=null;
   private static String local_username=null;
   private static String local_password=null;
    public static final String REMOTE_DBS="remotedb";
    public static final String REMOTE_TABS="remotetabs";
    public static final String REMOTE_REGIONS="remoteregion";
    public static final String REMOTE_FIELDS="reomtefield";
    public static final String WEB_VIEW="webview";

    public static  String current_db="default";
    public static  String  current_table="";
    public static String current_table_desc="";

    public static Stage mainWindow=null;

    public static String REMOTE="";
    public static final String LOCAL="LOCAL_";


   public static final java.util.Map<String,Object> CONTEXT=
           new java.util.HashMap<>();
   private static final Logger LOGGER= Logger.getLogger(App.class);

  static{
      Properties p=new Properties();
      try {
          p.load(new InputStreamReader(
                  App.class.getClassLoader().getResourceAsStream("db.properties")
                  ,"utf-8")
          );
          remote_hostname=CodeUtil.decode(p.getProperty("remote.hostname"));
          remote_username=CodeUtil.decode(p.getProperty("remote.username"));
          remote_password=CodeUtil.decode(p.getProperty("remote.password"));
          local_hostname=CodeUtil.decode(p.getProperty("local.hostname"));
          local_username=CodeUtil.decode(p.getProperty("local.username"));
          local_password=CodeUtil.decode(p.getProperty("local.password"));
      } catch (Exception e) {
          LOGGER.info("数据加载出错",e);
      }


  }




   /* public static final CommandLine CLIENT_REMOTE
            =new CommandLine(remote_hostname,remote_username,remote_password,"UTF-8");

    public static final CommandLine CLIENT_LOCAL
            =new CommandLine(local_hostname,local_username,local_password,"UTF-8");*/

    public static  String current_partition=null;
    @Override
    public void start(Stage primaryStage) throws Exception{

        mainWindow=primaryStage;



        Pane root = FXMLLoader.load(getClass().getResource("../../database.fxml"));
        primaryStage.setTitle("database-manager");
        Scene scene=new Scene(root,900,550);
        primaryStage.setScene(scene);

        primaryStage.getIcons().add(new Image(
               getClass().getResourceAsStream("../../bee.png")
                                             )
                                    );

        root.getChildren().sorted(new Comparator<Node>() {
            @Override
            public int compare(Node o1, Node o2) {
                double c=o1.getLayoutY()-o2.getLayoutY();
                if(c<0) return -1 ;
                else return  1;
            }
        }).filtered(node -> {

             String id=node.getId();
             if(REMOTE_DBS.equals(id)){
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
                 current_db=initCombox(REMOTE_DBS,node);
             }else if(REMOTE_TABS.equals(id)){
                 List<String> obj= null;
                 try {
                     obj = HttpUtil.getResult(
                             REMOTE+HttpUtil.SHOW_TABLES(),
                             "dbName="+current_db,
                             ListProcessData.getInstance());
                 } catch (Exception e) {
                     e.printStackTrace();
                 }
                 CONTEXT.put(REMOTE_TABS,obj);
                 //加载数据
                 current_table=initCombox(REMOTE_TABS,node);
             }else if(REMOTE_REGIONS.equals(id)){

                 /**
                  * 查询分区数目
                  * 1.抛出异常   可能其他原因导致接口报错
                  * 2.null       表示这个表可能为非分区表
                  * 2.空集合      代表这是一个分区表但是没有数据
                  * 3.非空集合     代表这个表是一个分区表且有数据
                  */
                 List<String> obj= null;
                 try {
                     obj = HttpUtil.getResult(
                             REMOTE+HttpUtil.SHOW_PARTITIONS(),
                             "dbName="+current_db+"&tableName="+current_table,
                             ListProcessData.getInstance());
                     Tuple2<String, String> pk_tdesc=TablePartionFiedAndDesc.getPartionFieldAndDesc();
                     current_table_desc=pk_tdesc._2;
                     if(obj==null){
                         LOGGER.info("`"+current_db+"`.`"+current_table+"` 是一个非分区表");
                         current_partition=null;
                     }else if(obj.isEmpty()){
                         LOGGER.info("`"+current_db+"`.`"+current_table+"` 是一个分区表,但表中没有数据");
                         current_partition=pk_tdesc._1;
                     }
                 } catch (Exception e) {
                    e.printStackTrace();
                 }
                 CONTEXT.put(REMOTE_REGIONS,obj);
                 //加载数据
                 initCombox(REMOTE_REGIONS,node);
             }else if(REMOTE_FIELDS.equals(id)){
                 initTableView(REMOTE_FIELDS,node);
             }else if("refreshid".equals(id)){
                 ProgressIndicator  progressIndicator= (ProgressIndicator) node;






             }/*elseif(WEB_VIEW.equals(id)){
                 WebView browser = (WebView) node;

                 WebEngine webEngine = browser.getEngine();
                 File html = new File("htmls/test.html");
                 try {
                     webEngine.load("file:///"+html.getCanonicalPath());
                     System.out.println("file:///"+html.getCanonicalPath());
                 } catch (IOException e) {
                     e.printStackTrace();
                 }
             }*/
             return false;

        });


        primaryStage.setOnCloseRequest(event -> {

            System.out.print("监听到窗口2关闭");
        });


        primaryStage.show();
    }

    public static void initTableView(String key,Node node) {

        TableView tableView= (TableView) node;

        ObservableList<TableColumn> cs=tableView.getColumns();
        ObservableList<Table> list = FXCollections.observableArrayList();
        List<Table> tabs= null;
        try {
            tabs = HttpUtil.getResult(
                    REMOTE+HttpUtil.GET_FIELDS(),
                    "dbName="+current_db+"&tableName="+current_table,
                     TableProcessData.getInstance(current_partition)
            );
        } catch (Exception e) {
            e.printStackTrace();
        }
        if(null==tabs || tabs.isEmpty()){
            tableView.setItems(list);
        }
        for (int i = 0; i < cs.size(); i++) {
            TableColumn column=cs.get(i);
            String name=column.getText();

             if("name".equals(name)){
                  column.setCellValueFactory(new PropertyValueFactory("fname"));
             }else if("type".equals(name)){
                 column.setCellValueFactory(new PropertyValueFactory("ftype"));
             }else if("desc".equals(name)){
                 column.setCellValueFactory(new PropertyValueFactory("fdesc"));
                 tableView.setEditable(true);
                 Callback<TableColumn<Object, String>, TableCell<Object, String>>
                         cell=TextFieldTableCell.forTableColumn();


                 column.setCellFactory(cell);



                 column.setOnEditCommit(new EventHandler<TableColumn.CellEditEvent>() {
                     @Override
                     public void handle(TableColumn.CellEditEvent event) {


                          Table value= (Table) event.getRowValue();
                          //如何判断一个字段是不是分区字段

                          String fname=value.getFname();
                          String ftype=value.getFtype();
                         String data="dbName="+current_db+"&sql=ALTER TABLE "+current_table;
                          if(null!=current_partition && current_partition.equals(fname)){
                              //修改注释
                              return;
                          }else if("#type".equals(ftype)){
                              //修改表注释
                              data+=" SET TBLPROPERTIES('comment' = '"+event.getNewValue()+"')";
                          }else{
                              //修改注释
                              data+=" CHANGE COLUMN ";

                              data+=value.getFname()+" "+fname+" "+value.getFtype()
                                      +" "+"COMMENT '"+event.getNewValue()+"'";
                          }
                         value.setFdesc(event.getNewValue().toString());

                         HttpUtil.execUpdate(REMOTE+HttpUtil.EXECUTE_UPDATE(),data);

                     }
                 });


             }
        }

        for (Table tab : tabs) {
            list.add(tab);
        }
       tableView.setItems(list);
    }


    public static String initCombox(String key,Node node) {
        try{
            List<String> list= (List<String>) CONTEXT.get(key);

            if(null==list ){
                ComboBox comboBox = (ComboBox) node;
                comboBox.setDisable(true);
                return null;
            }

            ComboBox comboBox = (ComboBox) node;
            comboBox.setDisable(false);
            ObservableList<String> ckvs=FXCollections.observableArrayList();
            for (String kv : list) {
                ckvs.add(kv);
            }
            comboBox.setItems(ckvs);
            if(null!=list && !list.isEmpty()) {
                comboBox.getSelectionModel().select(0);
                return  list.get(0);
            }else{
                return  null;
            }


        } catch (Exception e) {
           e.printStackTrace();
           throw e;
        }
    }


    public static void main(String[] args) {
        launch(args);
    }
}
