/*
	BASSenc_FLAC 2.4 Java class
	Copyright (c) 2017-2020 Un4seen Developments Ltd.

	See the BASSENC_FLAC.CHM file for more detailed documentation
*/

package com.un4seen.bass;

@SuppressWarnings({"all"})
public class BASSenc_FLAC
{
	// BASS_Encode_FLAC_NewStream flags
	public static final int BASS_ENCODE_FLAC_RESET = 0x1000000;

	public static native int BASS_Encode_FLAC_GetVersion();

	public static native int BASS_Encode_FLAC_Start(int handle, String options, int flags, BASSenc.ENCODEPROCEX proc, Object user);
	public static native int BASS_Encode_FLAC_StartFile(int handle, String options, int flags, String filename);
	public static native boolean BASS_Encode_FLAC_NewStream(int handle, String options, int flags);

	static {
		System.loadLibrary("bassenc");
		System.loadLibrary("bassenc_flac");
	}
}
