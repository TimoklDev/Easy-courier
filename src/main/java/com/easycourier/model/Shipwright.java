package com.easycourier.model;

public enum Shipwright
{
	JUNIOR_JIM("Junior Jim", Port.PANDEMONIUM),
	SAM("Shipwright Sam", Port.PORT_SARIM),
	SALLY("Shipwright Sally", Port.MUSA_POINT),
	SPENCER("Shipwright Spencer", Port.PORT_PISCARILIUS),
	SENNIA("Shipwright Sennia", Port.CIVITAS_ILLA_FORTIS),
	SILAS("Shipwright Silas", Port.ALDARIN),
	SCOTT("Shipwright Scott", Port.PORT_ROBERTS),
	SIGRUN("Shipwright Sigrun", Port.RELLEKKA),
	SORNIK("Shipwright Sornik", Port.DEEPFIN_POINT),
	SILRIEN("Shipwright Silrien", Port.PRIFDDINAS);

	private final String npcName;
	private final Port port;

	Shipwright(String npcName, Port port)
	{
		this.npcName = npcName;
		this.port = port;
	}

	public String getNpcName()
	{
		return npcName;
	}

	public Port getPort()
	{
		return port;
	}

	public static Shipwright at(Port port)
	{
		for (Shipwright shipwright : values())
		{
			if (shipwright.port == port)
			{
				return shipwright;
			}
		}
		return null;
	}
}
