package com.hbgj.http.util

import java.util
import java.util.regex.Pattern

import com.hbgj.App
import com.hbgj.code.CodeUtil
import com.hbgj.entity.Table
import org.slf4j.LoggerFactory
import scalaj.http.{Http, HttpOptions, HttpResponse}

import scala.util.control.Breaks
import scala.util.parsing.json.JSON

object HttpUtil {
       private val LOGGER=LoggerFactory.getLogger(this.getClass)
       private val CONTENT_TYPE="application/x-www-form-urlencoded"
       private val DEFAULT_CHARSET="UTF-8"
        private val URL_PREFIX=CodeUtil.decode("앞찾기온킨밀한긍래엘케련엘병목우좋은원은료글것몰애가앞나정릴감")
      private val URL_LOCAL_PREFIX=CodeUtil.decode("앞찾기온킨밀한위데대송랜롭칠많했서성밀패M늘빙두던희몰매빙장스크온림")



        val REMOTE_SHOW_CREATE_TABLE_KEY:String="result"

        val LOCAL_SHOW_CREATE_TABLE_KEY:String="createtab_stmt"


       val SHOW_DATABASES="show databases"
       val SHOW_TABLES="show tables"
       val SHOW_PARTITIONS="SHOW_PARTITIONS"
       val GET_FIELDS="GET FIELDS"
       val EXECUTE_UPDATE="EXECUTE_UPDATE"
       val EXECUTE_QUERY="EXECUTE_QUERY"

      val LOCAL_SHOW_DATABASES="LOCAL_show databases"
      val LOCAL_SHOW_TABLES="LOCAL_show tables"
      val LOCAL_SHOW_PARTITIONS="LOCAL_SHOW_PARTITIONS"
      val LOCAL_GET_FIELDS="LOCAL_GET FIELDS"
      val LOCAL_EXECUTE_UPDATE="LOCAL_EXECUTE_UPDATE"
      val LOCAL_EXECUTE_QUERY="LOCAL_EXECUTE_QUERY"

       val urls=Map[String,String](
         SHOW_DATABASES->     s"${URL_PREFIX}showDatabases",
        SHOW_TABLES->        s"${URL_PREFIX}showTables",
        SHOW_PARTITIONS->    s"${URL_PREFIX}showPartions",
        GET_FIELDS->         s"${URL_PREFIX}getFields",
        EXECUTE_UPDATE ->    s"${URL_PREFIX}exec",
        EXECUTE_QUERY->    s"${URL_PREFIX}exeQuery",
         LOCAL_SHOW_DATABASES->     s"${URL_LOCAL_PREFIX}showDatabases",
         LOCAL_SHOW_TABLES->        s"${URL_LOCAL_PREFIX}showTables",
         LOCAL_SHOW_PARTITIONS->    s"${URL_LOCAL_PREFIX}showPartions",
         LOCAL_GET_FIELDS->         s"${URL_LOCAL_PREFIX}getFields",
         LOCAL_EXECUTE_UPDATE ->    s"${URL_LOCAL_PREFIX}exec",
         LOCAL_EXECUTE_QUERY->      s"${URL_LOCAL_PREFIX}exeQuery"
       )

       @throws(classOf[Exception])
       def  getResult[T](key:String,data:String,parse:ProcessData[T]): T ={
         val result:HttpResponse[String] = Http(urls.get(key).get).postData(
           data).header("Content-Type",CONTENT_TYPE )
           .header("Charset", DEFAULT_CHARSET)
           .option(HttpOptions.readTimeout(10000)).asString
         val body = JSON.parseFull(result.body)
         body match {
           // Matches if jsonStr is valid JSON and represents a Map of Strings to Any
           case Some(map: Map[String, Any]) => {
             val map=body.get.asInstanceOf[Map[String, Any]]
             val success=map.get("success").get
             success match {
               case true =>{
                 val obj=map.get("data")
                  val rs=parse.parseObject(map.get("data").get)
                   rs
               }
               case _ =>{
                 throw new Exception(s"http request get answer failed ${result.body}")
               }
             }
           }
           case None =>{
             LOGGER.info("parse json get empty result",result.body)
             throw new Exception(s"parse json get empty result${result.body}")
           }
           case _ => {
             LOGGER.info("parse json get unexpected result",result.body)
             throw new Exception(s"parse json get unexpected result${result.body}")
           }
         }
       }
  def  execUpdate(key:String,data:String):Boolean={
    val result:HttpResponse[String] = Http(urls.get(key).get).postData(
      data).header("Content-Type",CONTENT_TYPE )
      .header("Charset", DEFAULT_CHARSET)
      .option(HttpOptions.readTimeout(10000)).asString
    val body = JSON.parseFull(result.body)
    body match {
      // Matches if jsonStr is valid JSON and represents a Map of Strings to Any
      case Some(map: Map[String, Any]) => {
        val map=body.get.asInstanceOf[Map[String, Any]]
        val success=map.get("success").get
        success match {
          case true =>{
               true
          }
          case _ =>{
                 LOGGER.info(s"http request get answer failed ${result.body}")
                 throw new Exception(s"http request get answer failed ${result.body}")
          }
        }

      }
      case None =>{
        LOGGER.info("parse json get empty result",result.body)
        throw new Exception(s"parse json get empty result${result.body}")
      }
      case _ => {
        LOGGER.info("parse json get unexpected result",result.body)
        throw new Exception(s"parse json get unexpected result${result.body}")
      }
    }
  }


}

trait ProcessData[T] {
  def parseObject(source: Any):T

}

class ListProcessData extends ProcessData[java.util.List[String]]{
  override def parseObject(source: Any): java.util.List[String] = {
      if(null==source)
              return   null
      val list=source.asInstanceOf[List[String]]
      val rs=new util.ArrayList[String];
      if(list.isEmpty)
             return  rs
      list.foreach(rs.add(_))
      rs
  }
}
object ListProcessData{
  val instance=new ListProcessData
  def getInstance: ListProcessData ={
           instance
  }
}


class TableProcessData extends  ProcessData[java.util.List[Table]]{
  var partionName:String=null
  override def parseObject(source: Any): java.util.List[Table] = {
    if(null==source)
      return   null
    val listMap=source.asInstanceOf[List[Map[String,String]]]
    val list:java.util.List[Table]=new util.ArrayList[Table]
    val loop=new Breaks
    loop.breakable(
        listMap.foreach(map=>{
            val fname=map.get("col_name").get
            val ftype=map.get("data_type")
            val fdesc=map.get("comment")
            if(fname.startsWith("#")) loop.break()
            if(partionName!=null && partionName.equals(fname)){
              val desc=fdesc.get;
              if(null==desc || desc.isEmpty)
                   list.add(new Table(fname,ftype.get,"分区字段-注释不可修改"))
              else
                list.add(new Table(fname,ftype.get,"分区字段-注释不可修改. "+fdesc.get))
            }else {
              if(null!=fname &&  !fname.isEmpty)
                    list.add(new Table(fname, ftype.get, fdesc.get))
            }
        })
    )
    //增加表的注释
    list.add(new Table("表注释","#type",App.current_table_desc))
    list
  }



}



object TableProcessData{
  val instance=new TableProcessData
  def getInstance(partionName:String): TableProcessData ={
    if(null!=partionName && !partionName.isEmpty) {
      instance.partionName = partionName
    }
    instance
  }
}

class ListMapProceessData extends ProcessData[java.util.List[util.HashMap[String,String]]]{
  override def parseObject(source: Any): util.List[util.HashMap[String,String]] = {
    if(null==source)
      return   null

    val list=source.asInstanceOf[List[Map[String,String]]]
    val rs=new util.ArrayList[util.HashMap[String,String]];
    if(list.isEmpty)
            return  rs

    list.foreach(maps=>{
       val map=new util.HashMap[String,String]();
       maps.foreach(
          x=> map.put(x._1,x._2)
       )
      rs.add(map)
    })
    rs
  }
}


object ListMapProceessData{
  val instance=new ListMapProceessData
  def getInstance: ListMapProceessData ={
    instance
  }
}


object TablePartionFiedAndDesc {
  def getPartionFieldAndDesc:(String,String)={
    var tableDesc=""
    var partitionField:String=null
    val list=HttpUtil.getResult(
      App.REMOTE+HttpUtil.EXECUTE_QUERY,
      s"dbName=${App.current_db}&sql= show create table   ${App.current_table}"  ,
      new ListMapProceessData)

    val range=0 to list.size()-1
    var partition=false
    for(i<-range){
      val map=list.get(i);

      if(null!=map && map.size()==1){
        var comment:String=""
        if(App.LOCAL.equals(App.REMOTE)){
          comment=map.get(HttpUtil.LOCAL_SHOW_CREATE_TABLE_KEY)
        }else {
          comment=map.get(HttpUtil.REMOTE_SHOW_CREATE_TABLE_KEY)
        }
        if(comment!=null && partition){
            val start=comment.indexOf("`");
            val end=comment.lastIndexOf("`")
            if(-1!=start && -1!=end && start<end){
               partitionField=comment.substring(start+1,end)
            }
        }
        partition=false
        if(comment!=null && comment.trim.startsWith("comment")){
          tableDesc=parseUnicodeStr(comment.replace("comment","").trim)
        }
        if(comment!=null && comment.contains("PARTITIONED BY (")){
            partition=true
        }
      }
    }
    (partitionField,tableDesc)
  }

  def decodeUnicode(dataStr: String): String = {
    var start = 0
    var end = 0
    val buffer = new StringBuffer
    while ( {
      start > -1
    }) {
      end = dataStr.indexOf("\\u", start + 2)
      var charStr = ""
      if (end == -1) charStr = dataStr.substring(start + 2, dataStr.length)
      else charStr = dataStr.substring(start + 2, end)
      val letter = Integer.parseInt(charStr, 16).toChar // 16进制parse整形字符串。
      buffer.append(new Character(letter).toString)
      start = end
    }
    buffer.toString
  }

  def parseUnicodeStr(string:String): String ={
    var str=string
    val reg="(\\\\[u][0-9A-Za-z]{4})*"
    val pattern=Pattern.compile(reg)
    val matcher=pattern.matcher(str)
    var index=0
    while(matcher.find()) {
      val s = matcher.group()
      if (!s.isEmpty) {
        // println(index,decodeUnicode(s))
        str=str.replace(s,decodeUnicode(s))
        index = index + 1
      }
    }

    str
  }

  def getTableDesc(dbName:String,tableName:String):String={
    var tableDesc=""
    val list=HttpUtil.getResult(
      App.REMOTE+HttpUtil.EXECUTE_QUERY,
      s"dbName=${dbName}&sql= desc formatted  ${tableName}"  ,
      new ListMapProceessData)

    val range=0 to list.size()-1

    for(i<-range){
      val map=list.get(i);
      if(null!=map && map.size()==1){
        var comment:String=""
        if(App.LOCAL.equals(App.REMOTE)){
          comment=map.get(HttpUtil.LOCAL_SHOW_CREATE_TABLE_KEY)
        }else {
          comment=map.get(HttpUtil.REMOTE_SHOW_CREATE_TABLE_KEY)
        }
        if(comment!=null && comment.trim.startsWith("comment")){
          tableDesc=parseUnicodeStr(comment.replace("comment","").trim)
        }
      }
    }
    tableDesc
  }
}


