package com.easycourier.service;

import com.easycourier.model.Port;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import net.runelite.api.coords.WorldPoint;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class SeaNetworkGraphTest
{
	@Test
	public void readsExportedPluginGraphAndFindsRoute()
	{
		String document = "{\n"
			+ "  \"format\": \"easy-courier-sea-network\",\n"
			+ "  \"schemaVersion\": 1,\n"
			+ "  \"nodes\": [\n"
			+ "    {\"id\": -6, \"port\": \"ALDARIN\", \"x\": 1454, \"y\": 2977, \"plane\": 0, \"connections\": [1]},\n"
			+ "    {\"id\": 1, \"port\": null, \"x\": 1600, \"y\": 3100, \"plane\": 0, \"connections\": [-6, -9]},\n"
			+ "    {\"id\": -9, \"port\": \"PORT_ROBERTS\", \"x\": 1858, \"y\": 3307, \"plane\": 0, \"connections\": [1]}\n"
			+ "  ]\n"
			+ "}";
		SeaNetwork network = new SeaNetwork(new ByteArrayInputStream(document.getBytes(StandardCharsets.UTF_8)), null);
		List<WorldPoint> path = network.path(Port.ALDARIN, Port.PORT_ROBERTS);
		assertEquals(Port.ALDARIN.getMapPoint(), path.get(0));
		assertEquals(Port.PORT_ROBERTS.getMapPoint(), path.get(path.size() - 1));
		assertTrue(path.contains(new WorldPoint(1600, 3100, 0)));
		assertTrue(network.distance(Port.ALDARIN, Port.PORT_ROBERTS)
			> Port.ALDARIN.getMapPoint().distanceTo2D(Port.PORT_ROBERTS.getMapPoint()));
	}
}
