/**
 * 
 */
package akkamud;

import java.util.ArrayList;

/**
 * @author stephen.ayotte
 *
 */
class TelnetMenu
{
	private ArrayList<ArrayList<Object>> menuItems;
	
	public TelnetMenu()
	{
		menuItems = new ArrayList<ArrayList<Object>>();
	}
	
	public void addMenuItem(Object selection, String description)
	{
		ArrayList<Object> item = new ArrayList<Object>();
		item.add(selection);
		item.add(description);
		menuItems.add(item);
	}
	
	public String buildMenuString()
	{
		String menu = "";
		int index = 0;
		for(ArrayList<Object> item: menuItems)
		{
			String description = (String)item.get(1);
			menu += "["+index+"] "+description+"\r\n";
			index++;
		}
		return menu;
	}
	
	public Object getSelection(int index)
	{
		return menuItems.get(index).get(0);
	}
}
