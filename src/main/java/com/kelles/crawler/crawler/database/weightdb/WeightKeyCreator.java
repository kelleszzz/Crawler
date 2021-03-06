package com.kelles.crawler.crawler.database.weightdb;

import com.kelles.crawler.crawler.util.*;
import com.sleepycat.bind.serial.SerialBinding;
import com.sleepycat.je.DatabaseEntry;
import com.sleepycat.je.SecondaryDatabase;
import com.sleepycat.je.SecondaryKeyCreator;

public class WeightKeyCreator <ValueObjectClass extends WeightInterface> implements SecondaryKeyCreator{
	public SerialBinding serialBinding;

	public WeightKeyCreator(SerialBinding serialBinding) {
		super();
		this.serialBinding = serialBinding;
	}
	

	@Override
	public boolean createSecondaryKey(SecondaryDatabase secDb,
    DatabaseEntry keyEntry, 
    DatabaseEntry dataEntry,
    DatabaseEntry resultEntry) {
		ValueObjectClass obj=(ValueObjectClass) serialBinding.entryToObject(dataEntry);
		resultEntry.setData(Util.intToByteArray(obj.getWeight()));
		return true;
	}
}
