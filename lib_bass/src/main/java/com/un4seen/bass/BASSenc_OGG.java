/*
	BASSenc_OGG 2.4 Java class
	Copyright (c) 2016-2020 Un4seen Developments Ltd.

	See the BASSENC_OGG.CHM file for more detailed documentation
*/

package com.un4seen.bass;

@SuppressWarnings({"all"})
public class BASSenc_OGG
{
	// BASS_Encode_OGG_NewStream flags
	public static final int BASS_ENCODE_OGG_RESET = 0x1000000;
	
	public static native int BASS_Encode_OGG_GetVersion();

	public static native int BASS_Encode_OGG_Start(int handle, String options, int flags, BASSenc.ENCODEPROC proc, Object user);
	public static native int BASS_Encode_OGG_StartFile(int handle, String options, int flags, String filename);
	public static native boolean BASS_Encode_OGG_NewStream(int handle, String options, int flags);

	static {
		System.loadLibrary("bassenc");
		System.loadLibrary("bassenc_ogg");
	}
}
