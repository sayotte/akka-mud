package akkamud;

import java.io.Serializable;

import akka.actor.ActorRef;

abstract class Announce implements Serializable
{
	static final long serialVersionUID = 1;
}

class Entry extends Announce
{
	public ActorRef who;
	public Entry(ActorRef actor)
	{
		super();
		who = actor;
	}
}

class Exit extends Announce
{
	public ActorRef who;
	public Exit(ActorRef actor)
	{
		super();
		who = actor;
	}
}