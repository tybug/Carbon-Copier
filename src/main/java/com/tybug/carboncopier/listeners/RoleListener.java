package com.tybug.carboncopier.listeners;

import com.tybug.carboncopier.Hub;

import net.dv8tion.jda.core.events.role.RoleCreateEvent;
import net.dv8tion.jda.core.events.role.RoleDeleteEvent;
import net.dv8tion.jda.core.events.role.update.GenericRoleUpdateEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;

public class RoleListener extends ListenerAdapter {


	@SuppressWarnings("rawtypes")
	@Override
	public void onGenericRoleUpdate(GenericRoleUpdateEvent event) {	
		if(Hub.isTargetGuild(event.getGuild().getId())) {
			return;
		}
		Hub.updateRole(event.getRole());
	}
	
	
	
	@Override
	public void onRoleCreate(RoleCreateEvent event) {
		if(Hub.isTargetGuild(event.getGuild().getId())) {
			return;
		}
		Hub.createRole(event.getRole());
	}
	
	
	
	@Override
	public void onRoleDelete(RoleDeleteEvent event) {
		if(Hub.isTargetGuild(event.getGuild().getId())) {
			return;
		}
		Hub.deleteRole(event.getRole());
	}
}
