package com.hbgj;

import com.hbgj.code.CodeUtil;
import com.hbgj.entity.Table;
import com.hbgj.http.util.HttpUtil;
import com.hbgj.http.util.ListProcessData;
import com.hbgj.http.util.TableProcessData;
import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import javafx.util.Callback;
import org.apache.log4j.Logger;
import java.io.InputStreamReader;
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

    public static  String current_db="default";
    public static  String  current_table="";



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
          //e.printStackTrace();
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

        Pane root = FXMLLoader.load(getClass().getResource("../../database.fxml"));
        primaryStage.setTitle("database-manager");
        Scene scene=new Scene(root,900,550);
        primaryStage.setScene(scene);

        root.getChildren().filtered(node -> {

             String id=node.getId();
             if(REMOTE_DBS.equals(id)){
                 //加载数据
                 List<String> obj=HttpUtil.getResult(HttpUtil.SHOW_DATABASES(), "",
                         ListProcessData.getInstance()
                 );
                 CONTEXT.put(REMOTE_DBS,obj);
                 current_db=initCombox(REMOTE_DBS,node);
             }else if(REMOTE_TABS.equals(id)){
                 List<String> obj=HttpUtil.getResult(HttpUtil.SHOW_TABLES(),
                         "dbName="+current_db,
                         ListProcessData.getInstance());
                 CONTEXT.put(REMOTE_TABS,obj);
                 //加载数据
                 current_table=initCombox(REMOTE_TABS,node);
             }else if(REMOTE_REGIONS.equals(id)){

                 List<String> obj=HttpUtil.getResult(HttpUtil.SHOW_PARTITIONS(),
                         "dbName="+current_db+"&tableName="+current_table,
                         ListProcessData.getInstance());
                 CONTEXT.put(REMOTE_REGIONS,obj);
                 if(null!=obj && !obj.isEmpty()){
                     current_partition=obj.get(0).split("=")[0];
                 }else{
                     current_partition=null;
                 }
                 //加载数据
                 initCombox(REMOTE_REGIONS,node);
             }else if(REMOTE_FIELDS.equals(id)){


                 initTableView(REMOTE_FIELDS,node);


             }else if("localdb".equals(id)){
                 //initCombox("local-dbs",node);
             }
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
        List<Table> tabs=HttpUtil.getResult(
                 HttpUtil.GET_FIELDS(),
                "dbName="+current_db+"&tableName="+current_table,
                 TableProcessData.getInstance(current_partition)
        );
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
                         String data="dbName="+current_db+"&sql=ALTER TABLE "+current_table;
                          if(null!=current_partition && current_partition.equals(fname)){
                              //修改注释
                              data+="  SET TBLPROPERTIES('comment' = '"+event.getNewValue()+"')";
                             // return ;
                          }else{
                              //修改注释
                              data+=" CHANGE COLUMN ";

                              data+=value.getFname()+" "+fname+" "+value.getFtype()
                                      +" "+"COMMENT '"+event.getNewValue()+"'";
                          }
                          value.setFdesc(event.getNewValue().toString());

                         HttpUtil.changeComment(HttpUtil.EXECUTE_UPDATE(),data);


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
