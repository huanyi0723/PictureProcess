package com.example.pictureprocess.common;

public class Sticker {

	public int fatherId = 0; 		             //M	大类Id
	public String fatherName = "";		//M	贴纸名
	public int sonId	= 0 ;                    //M	小类Idl
	public String sonName	= "";	        //M	小类mingc
	public String pasterName	= "";    //M	贴纸名称
	public int pasterId	= 0;                //M	贴纸ID
	public String pasterUrl	= "";	       //M	贴纸Url
	public String pictureUrl	 = "";      //M	范例Url
	public int useCount = 0;               
	
	public boolean isNeedShare = false;
	public String pasterLocalpath	 = "";      //M	贴纸本地路径
	
	public int drawableId; //图像ID
	
	public Sticker(int drawableId) {
		this.drawableId = drawableId;
	}
}
