package com.hbgj.remote

import java.io.{BufferedReader, InputStreamReader}
import java.nio.charset.Charset

import com.jcraft.jsch.{Channel, ChannelExec, JSch, Session}

import scala.util.control.Breaks

class CommandLine(val hostname:String,val username:String,val password:String,val charset:String="utf-8") {

   private var  jsch:JSch=null;

   private var session:Session=null;


  /**
    * 连接到指定的IP
    *
    */
  def connect() {
    jsch = new JSch()
    session = jsch.getSession(username, hostname, 22)
    session.setPassword(password);
    val config = new java.util.Properties()
    config.put("StrictHostKeyChecking", "no")
    session.setConfig(config)
    session.connect()
  }

  connect
  /**
    * 执行相关的命令
    */
  @throws(classOf[Exception])
  def exec(commandLine:String):String={
    val sb=new StringBuffer()
    var reader:BufferedReader = null
    var channel:ChannelExec  = null
    try {
      channel = session.openChannel("exec").asInstanceOf[ChannelExec]
       channel.setCommand(commandLine);
      channel.setInputStream(null);
      channel.setErrStream(System.err);

      channel.connect();
      val in = channel.getInputStream()
      reader = new BufferedReader(new InputStreamReader(in, Charset.forName(charset)));
      var buf:String = null
      val loop=new Breaks
      loop.breakable(

        while (true) {
          buf=reader.readLine()
          if(null==buf)loop.break
          sb.append(buf).append("\n")
        }
      )
      sb.toString
    } catch  {
      case e:Exception=>
         throw new Exception("执行命令"+commandLine+"时抛出异常",e);
    } finally {
      try {
        reader.close()
        channel.disconnect()
      } catch  {
        case e:Exception=>
          throw new Exception("当关闭channel时抛出异常",e);
      }
    }
  }
  def releaseSession: Unit ={
       session.disconnect
  }

}

