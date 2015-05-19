package com.example.customdownload.util;

import java.text.DecimalFormat;

public class CommonTool {	
	//type为0表示只显示大小，不显示速度，type为1表示显示速度
	public static String convertBSize2String(long size_b, int type)
	{
		DecimalFormat df = new DecimalFormat("#.0");
		String strRet = "";
		if(size_b < 1024)
			strRet = String.valueOf(size_b) + "B" + ((type==1)? "/S":"");               //B的舍去小数部分
		else if(size_b < 1024*1024)
			strRet = df.format((double)size_b/1024) +"KB" + ((type==1)? "/S":""); 
		else
			strRet = df.format((double)size_b/(1024*1024)) + "MB" + ((type==1)? "/S":"");          //MB的保留一位小数
		return strRet;
	}
}
