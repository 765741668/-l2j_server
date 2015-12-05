/*
 * Copyright (C) 2004-2015 L2J Server
 * 
 * This file is part of L2J Server.
 * 
 * L2J Server is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * L2J Server is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package com.l2jserver.commons.properties;

/**
 * @author HorridoJoho
 * @param <T> the property type
 */
public class Property<T>
{
	private T _value;
	private boolean _dirty;
	
	public Property(T initialValue)
	{
		_value = initialValue;
		_dirty = true;
	}
	
	public void set(T newValue)
	{
		_value = newValue;
		_dirty = true;
	}
	
	public T get()
	{
		return _value;
	}
	
	public T consume()
	{
		_dirty = false;
		return get();
	}
	
	public final boolean isDirty()
	{
		return _dirty;
	}
}