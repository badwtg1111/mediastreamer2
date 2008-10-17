/*
p2pproxy
Copyright (C) 2007  Jehan Monnier ()

P2pAutoConfigTester.java - .

This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation; either version 2
of the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
*/
package org.linphone.p2pproxy.test.utils;

import java.io.File;
import java.net.DatagramSocket;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.log4j.Logger;
import org.linphone.p2pproxy.api.P2pProxyException;
import org.linphone.p2pproxy.api.P2pProxyResourceManagement;
import org.linphone.p2pproxy.core.P2pProxyMain;
import org.linphone.p2pproxy.launcher.P2pProxylauncherConstants;
import org.zoolu.net.SocketAddress;
import org.zoolu.sip.provider.SipProvider;
import org.zoolu.sip.provider.SipStack;

public class UserInstance {
private final Thread mFonisThread;
private Timer mTimer;
private final SipProvider mProvider;
private final SipClient mSipClient;
private final int REGISTRATION_PERIOD=60;
private final static Logger mLog = Logger.getLogger(UserInstance.class);
public UserInstance(final String userName) throws  P2pProxyException {
	try {
	DatagramSocket lSocket = new DatagramSocket();
	lSocket.setReuseAddress(true);
	int lSipPort = lSocket.getLocalPort();
	lSocket.close();
	final String[] lParam = {"-jxta userinstance-"+userName
							," -edge-only"
							," -seeding-rdv tcp://82.67.74.86:9701"
							," -seeding-relay tcp://82.67.74.86:9701"};
	lSocket.close();
	
	Runnable lFonisTask = new Runnable() {
		public void run() {
			P2pProxyMain.main(lParam);
		}
		
	};
	mFonisThread = new Thread(lFonisTask,"fonis lib");
	mFonisThread.start();
	int lRetry=0;
	while (P2pProxyMain.getState() != P2pProxylauncherConstants.P2PPROXY_CONNECTED && lRetry++<20) {
		Thread.sleep(500);
	}
	if (P2pProxyMain.getState() != P2pProxylauncherConstants.P2PPROXY_CONNECTED) {
		throw new P2pProxyException("Cannot connect to fonis network");
	}
	P2pProxyMain.createAccount(userName);
	SipStack.log_path = "userinstance-"+userName+"/logs";
	File lFile = new File(SipStack.log_path);
    if (lFile.exists() == false) lFile.mkdir();
	mProvider=new SipProvider(null,lSipPort);
	mSipClient = new SipClient(mProvider,userName,30000);
	final TimerTask lTimerTask = new  TimerTask() {
		@Override
		public void run() {
			try {
			// 1 get proxy address
			String lProxyUri = P2pProxyMain.lookupSipProxyUri(P2pProxyResourceManagement.DOMAINE);
			//2 setOutbound proxy
			mProvider.setOutboundProxy(new SocketAddress(lProxyUri));
			mSipClient.register(REGISTRATION_PERIOD,userName);
			} catch(Exception e) {
				mLog.error("cannot register user["+userName+"]",e);
			} finally {
				mTimer.schedule(this, REGISTRATION_PERIOD-REGISTRATION_PERIOD/10);
			}
		}
		
	};
	mTimer.schedule(lTimerTask, REGISTRATION_PERIOD-REGISTRATION_PERIOD/10);
	mSipClient.listen();
	} catch (Exception e) {
		throw new P2pProxyException("cannot start client",e);
	}
}
public void call(String aTo, int duration) {
	mSipClient.call(aTo, true, duration);
}
static int main(String[] args) throws P2pProxyException {
	String lFrom="sip:toto", lTo;
	int lDuration = 10, lLoop=1;
	for (int i=0; i < args.length; i=i+2) {  
		   String argument = args[i];
		   if (argument.equals("-from")) {
			   lFrom = args[i + 1];
			   System.out.println("from [" + lFrom + "]");
			   //nop
		   } else if (argument.equals("-to")) {
			   lTo = args[i + 1];
			   System.out.println("to [" + lTo + "]");
			   
		   } else if (argument.equals("-duration")) {
			   lDuration =  Integer.parseInt(args[i + 1]);
			   System.out.println("duration [" + lDuration + "]");
			   
		   } else if (argument.equals("-nb-call")) {
			   lLoop =  Integer.parseInt(args[i + 1]);
			   System.out.println("nb-call [" + lLoop + "]");
			   
		   } else {
			   System.out.println("Invalid option: " + args[i]);
			   usage();
			   System.exit(1);
		   }
	   }	UserInstance lUserInstance= new UserInstance(lFrom);
	   for (int i=0;i<lLoop;i++) {
		   lUserInstance.call(lTo, lDuration);
	   }
	   
}
private static void usage() {
	// TODO Auto-generated method stub
	
}
}