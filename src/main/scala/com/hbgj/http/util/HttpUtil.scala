package com.hbgj.http.util

import java.util

import com.hbgj.entity.Table
import org.slf4j.LoggerFactory
import scalaj.http.{Http, HttpOptions, HttpResponse}

import scala.util.control.Breaks
import scala.util.parsing.json.JSON

object HttpUtil {
       private val LOGGER=LoggerFactory.getLogger(this.getClass)
       private val CONTENT_TYPE="application/x-www-form-urlencoded"
       private val DEFAULT_CHARSET="UTF-8"
       private val URL_PREFIX="http://47.93.56.114:19997/hive/"

        val SHOW_DATABASES="show databases"
       val SHOW_TABLES="show tables"
       val SHOW_PARTITIONS="SHOW_PARTITIONS"
       val GET_FIELDS="GET FIELDS"
       val EXECUTE_UPDATE="EXECUTE_UPDATE"
       val EXECUTE_QUERY="EXECUTE_QUERY"

       val urls=Map[String,String](
            SHOW_DATABASES->     s"${URL_PREFIX}showDatabases",
            SHOW_TABLES->        s"${URL_PREFIX}showTables",
            SHOW_PARTITIONS->    s"${URL_PREFIX}showPartions",
            GET_FIELDS->         s"${URL_PREFIX}getFields",
            EXECUTE_UPDATE ->    s"${URL_PREFIX}exec",
              EXECUTE_QUERY->    s"${URL_PREFIX}exeQuery"
       )
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


  def  changeComment(key:String,data:String):Boolean={
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

            }else
               list.add(new Table(fname,ftype.get,fdesc.get))
        })
    )
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