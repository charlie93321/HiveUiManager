package com.hbgj.remote



import java.net.URLEncoder
import java.util
import java.util.function.Consumer
import java.util.regex.Pattern

import com.hbgj.code.CodeUtil
import com.hbgj.http.util.{HttpUtil, ListMapProceessData, ListProcessData, TableProcessData}
/**
  * private String charset = "UTF-8"; // 设置编码格式
  * private String user; // 用户名
  * private String passwd; // 登录密码
  * private String host; // 主机IP
  * private JSch jsch;
  * private Session session;
  *
  */
/*class ShellCommand(passwod:String,user:String,charset:String="utf-8") {

}*/

object t1{
  def main(args: Array[String]): Unit = {
/*

     val commandLine=new CommandLine("charlie.hdp.com","hadoop","hadoop");

    val str=commandLine.exec("hive -e 'show databases'")


    println(str)

    commandLine.releaseSession*/

/*    val code=CodeUtil.encode("sxljldh@1387")
    val decode=CodeUtil.decode(code);
    println(code,decode)*/
    /**
      * url: jdbc:hive2://master:50098/default
      * driver-class-name: org.apache.hive.jdbc.HiveDriver
      * type: org.apache.tomcat.jdbc.pool.DataSource
      * user: bigdata
      * password: sxljldh@1387
      */
   /* val code=CodeUtil.encode("sxljldh@1387")
    val decode=CodeUtil.decode(code);
    println(code,decode)*/



    val url = "http://47.93.56.114:19997/hive/exeQuery"
   // val pipeline: HttpRequest => Future[HttpResponse] = sendReceive

    // Post with header and parameters
   // val responseFuture1: Future[String] = pipeline(Post(Uri(url) withParams ("param" -> paramValue), yourPostData) map (_.entity.asString)

    // Post with header
   // val responseFuture2: Future[String] = pipeline(Post(url, yourPostData)) map (_.entity.asString)



    /*val mediaType=MediaTypes.`application/x-www-form-urlencoded`
    val contentType=ContentType(mediaType, HttpCharsets.`UTF-8`)

    val entity=HttpEntity(contentType,"dbName=default&sql='show create table  sp_class.areaby_latlnt'")
    val req = HttpRequest(HttpMethods.POST,Uri(url),Nil,entity)
dbName=default&sql= desc  sp_class.areaby_latlnt
    HttpResponse.apply().*/

   /* val list=HttpUtil.getResult(HttpUtil.EXECUTE_QUERY,
      "dbName=default&sql= desc formatted  sp_class.tag_user_model_gt_action"  ,
      new ListMapProceessData)

    val range=0 to list.size()-1

    for(i<-range){
       val map=list.get(i);
       if(null!=map && map.size()==1){
          val comment=map.get("result")

          if(comment.trim.startsWith("comment")){
               val reg=
          }
       }
    }*/


   //println(decodeUnicode(str))



  }


}
