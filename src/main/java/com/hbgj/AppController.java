package com.hbgj;

import com.hbgj.http.util.HttpUtil;
import com.hbgj.http.util.ListProcessData;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import com.hbgj.App.*;
import javafx.scene.control.TableView;

import java.util.List;

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

    public void changeDb(){
        current_db=remotedb.getValue().toString();
        Object obj=HttpUtil.getResult(HttpUtil.SHOW_TABLES(),
                "dbName="+current_db,
                ListProcessData.getInstance());
        CONTEXT.put(REMOTE_TABS,obj);
        if(null!=obj){
             List<String> tabs= (List<String>) obj;
             current_table=tabs.get(0);
        }
        //加载数据
        current_table=App.initCombox(REMOTE_TABS,remotetabs);
    }

    public void changeTabs(ActionEvent actionEvent) {
        Object table=remotetabs.getValue();
        if(null!=table)current_table=table.toString();
        Object obj=HttpUtil.getResult(HttpUtil.SHOW_PARTITIONS(),
                "dbName="+current_db+"&tableName="+current_table,
                ListProcessData.getInstance());
        CONTEXT.put(REMOTE_REGIONS,obj);
        //加载数据
        initCombox(REMOTE_REGIONS,remoteregion);
        initTableView(REMOTE_FIELDS,reomtefield);

    }
}
