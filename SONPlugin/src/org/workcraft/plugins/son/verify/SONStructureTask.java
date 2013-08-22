package org.workcraft.plugins.son.verify;

import java.awt.Color;

import org.apache.log4j.Logger;
import org.workcraft.plugins.son.ONGroup;
import org.workcraft.plugins.son.SONModel;
import org.workcraft.plugins.son.gui.StructureVerifySettings;
import org.workcraft.tasks.ProgressMonitor;
import org.workcraft.tasks.Result;
import org.workcraft.tasks.Task;
import org.workcraft.tasks.Result.Outcome;

public class SONStructureTask implements Task<VerificationResult>{

	private SONModel net;
	private Logger logger = Logger.getLogger(this.getClass().getName());

	private StructureVerifySettings settings;
	private int totalErrNum;
	private int totalWarningNum = 0;

	public SONStructureTask(StructureVerifySettings settings, SONModel net){
		this.settings = settings;
		this.net = net;
	}

	@Override
	public Result<? extends VerificationResult> run (ProgressMonitor <? super VerificationResult> monitor){

		clearConsole();

		//group structure task
		if(settings.getType() == 0){
			GroupStructureTask groupSTask = new GroupStructureTask(net);

			for(ONGroup group : settings.getSelectedGroups()){
				//main group task
				groupSTask.task(group);

				//highlight setting
				if(settings.getErrNodesHighlight()){
					groupSTask.errNodesHighlight();
				if (groupSTask.hasErr())
					group.setForegroundColor(Color.RED);
				}
			}
			totalErrNum = groupSTask.getErrNumber();
			totalWarningNum = groupSTask.getWarningNumber();
		}

		//CSON structure task
		if(settings.getType() == 1){
			CSONStructureTask csonSTask = new CSONStructureTask(net);
			csonSTask.Task(settings.getSelectedGroups());

			if(settings.getErrNodesHighlight()){
				csonSTask.errNodesHighlight();
			}
			totalErrNum = totalErrNum + csonSTask.getErrNumber();
			totalWarningNum = totalWarningNum + csonSTask.getWarningNumber();
		}

		//BSON structure task
		if(settings.getType() == 2){
			BSONStructureTask bsonSTask = new BSONStructureTask(net);
			bsonSTask.Task(settings.getSelectedGroups());

			if(settings.getErrNodesHighlight()){
				bsonSTask.errNodesHighlight();
			}
			totalErrNum = totalErrNum + bsonSTask.getErrNumber();
			totalWarningNum = totalWarningNum + bsonSTask.getWarningNumber();
		}

		logger.info("\n\nVerification-Result : "+ this.getTotalErrNum() + " Error(s), " + this.getTotalWarningNum() + " Warning(s).");

		return new Result<VerificationResult>(Outcome.FINISHED);
	}

	private static void clearConsole()
	{
	    try
	    {
	        String os = System.getProperty("os.name");

	        if (os.contains("Window"))
	        {
	            Runtime.getRuntime().exec("cls");
	        }
	        else
	        {
	            Runtime.getRuntime().exec("cls");
	        }
	    }
	    catch (Exception exception)
	    {
	        //  Handle exception.
	    }
	}

	public int getTotalErrNum(){
		return this.totalErrNum;
	}

	public int getTotalWarningNum(){
		return this.totalWarningNum;
	}

}
