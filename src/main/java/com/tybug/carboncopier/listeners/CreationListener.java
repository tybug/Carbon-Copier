package com.tybug.carboncopier.listeners;

import com.tybug.carboncopier.Hub;

import net.dv8tion.jda.core.events.role.RoleCreateEvent;
import net.dv8tion.jda.core.events.role.RoleDeleteEvent;
import net.dv8tion.jda.core.events.role.update.GenericRoleUpdateEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;

public class CreationListener extends ListenerAdapter {


	@SuppressWarnings("rawtypes")
	@Override
	public void onGenericRoleUpdate(GenericRoleUpdateEvent event) {		
		Hub.updateRole(event.getRole());
	}
	
	
	
	@Override
	public void onRoleCreate(RoleCreateEvent event) {
		// TODO create new role, link two in db
	}
	
	
	
	@Override
	public void onRoleDelete(RoleDeleteEvent event) {
		// TODO delete old role, remove link from db
	}
}
