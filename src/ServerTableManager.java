import java.util.Hashtable;
import java.util.List;
import java.util.Vector;

public class ServerTableManager {
	private Hashtable<Integer, ServerTable> m_tableMap;
	List<ServerTable> tables = new Vector<ServerTable>();
	
	public ServerTableManager(){}
	
	public synchronized void addTable(ServerTable table){
		// Loop over to find open slot, or add to end.
		// The id of the table is set to index of this list.
		short idx = 0;
		while (idx < tables.size() && tables.get(idx) != null){
			idx ++;
		}
		table.setId(idx);
		if (idx == tables.size()){
			tables.add(table);
		}
		else{
			tables.set(idx, table);
		}
	}
}