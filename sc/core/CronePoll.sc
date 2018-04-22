// define and execute a poll
CronePoll {
	var task; // Task; timing thread
	var <period; // Number; callback interval
	var function; // Function; what produces the value/data
	var <name; // Symbol; label for outside world
	var <index;  // Integer; another label, for ordering/efficiency
	var <type; 	// Symbol; either \value or \data
	var isRunning; 	// Boolean; running state
	var <oscPath; 	// Symbol, full destination path
	var <>oscAddr; 	// NetAddr; address to send to.
	var callback; 	// Function; full (private) callback function
	var periodic; // Boolean; is this a scheduled/repeating poll or not

	// create and define a new poll.
	// function argument should return value or data
	// type should be \value or \data
	*new { arg index, name, function = {}, period=0.25, type=\value, periodic=true;
		^super.new.init(index, name, function, period, type, periodic);
	}

	init { arg argIndex, argName, argFunction, argPeriod, argType, argPeriodic;
		index = argIndex;
		name = argName;
		period = argPeriod;
		type = argType;
		function = argFunction;
		periodic = argPeriodic;
		if(type == \value, {
			oscPath = '/poll/value';
		}, {
			oscPath = '/poll/data';
		});
		callback = {
			perform(function.value);
		};
		if(periodic, {
			task = Task { inf.do {
				callback.value;
				period.wait;
			}};

		});
		isRunning = false;		
	}

	start { arg addr;
		oscAddr = addr;
		if(isRunning.not, {
			isRunning = true;
			task.play;
		});
	}

	stop {
		if(isRunning, {
			isRunning = false;
			task.stop;
		});
	}

	setTime { arg time;
		period = time;
	}

	// send the poll's message with value
	// non-periodic polls can call this explicitly
	perform {
		arg val;
		oscAddr.sendMsg(oscPath, index, val);
	}
}


// singleton registry of available polls
CronePollRegistry {
	classvar polls;
	classvar pollNames;

	*initClass {
		polls = Dictionary.new;
		pollNames = Dictionary.new;
	}
	//	*initClass { polls = Dictionary.new; }

	// create a CronePoll and add to the registry
	*register { arg name, func, dt=0.25, type=\value, periodic=true;
		name = name.asSymbol;
		if(polls.keys.includes(name), {
			postln("warning: attempted to add poll using existing key");
			^false;
		}, {
			pollNames[polls.size] = name;
			polls[name] = CronePoll.new(polls.size, name, func, dt, type, periodic);
			^true;
		});

	}

	*remove { arg name;
		name = name.asSymbol;
		if(polls.keys.includes(name), {
			polls[name].stop;
			pollNames.removeAt(polls[name].idx);
			polls.removeAt(name);
		})
	}

	*clear {
		polls.do({ |p| p.stop; });
		polls.clear;
	}

	*getPollFromIdx { arg idx;
//		postln("getting poll for idx: " ++ idx);
//		postln("poll names: " ++ pollNames);
		^polls[pollNames[idx]];
	}

	*getPollFromName { arg name;
		^polls[name];
	}

	*getNumPolls { ^polls.size }

}

