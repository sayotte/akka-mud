package akkamud;

import java.io.Serializable;

import akka.actor.ActorRef;

class Announce implements Serializable
{
	public Object payload;
    public Announce(Object obj){ payload = obj; }    
}

class Entry implements Serializable
{
	public ActorRef who;
	public Entry(ActorRef actor){ who = actor; }
}

class Exit implements Serializable
{
	public ActorRef who;
	public Exit(ActorRef actor){ who = actor; }
}