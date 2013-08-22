package org.workcraft.plugins.son.test;

import org.apache.log4j.Logger;
import org.workcraft.plugins.son.OutputRedirect;
import org.workcraft.plugins.son.connections.VisualSONConnection.SONConnectionType;
import org.workcraft.plugins.son.verify.VerificationResult;
import org.workcraft.tasks.ProgressMonitor;
import org.workcraft.tasks.Result;
import org.workcraft.tasks.Task;
import org.workcraft.tasks.Result.Outcome;

public class test implements Task<VerificationResult> {

	private Logger logger = Logger.getLogger(this.getClass().getName());

	@Override
	public Result<? extends VerificationResult> run (ProgressMonitor <? super VerificationResult> monitor){
		try{
		output();
		}catch (Exception e){
			return new Result<VerificationResult>(Outcome.FAILED);
		}

		return new Result<VerificationResult>(Outcome.FINISHED);
	}

	public void output() throws Exception{

		OutputRedirect.Redirect();


		int i = 0;
        while (i < 10)
        {
            logger.error ("Current time: " + System.currentTimeMillis ());
            Thread.sleep (1000L);
            i++;
        }
	}

	public enum SONConnectionType
	{
		POLYLINE,
		BEZIER,
		SYNCLINE,
		ASYNLINE,
		BHVLINE;

		public String getTypetoString(SONConnectionType type){
			if (type == SONConnectionType.POLYLINE)
				return "POLY";
			if (type == SONConnectionType.SYNCLINE)
				return "SYNC";
			if (type == SONConnectionType.ASYNLINE)
				return "ASYN";
			if (type == SONConnectionType.BHVLINE)
				return "BHV";
			return "";
		}
	};

	public static void main(String[] arg){
		System.out.println(SONConnectionType.SYNCLINE.toString());
	}


}
