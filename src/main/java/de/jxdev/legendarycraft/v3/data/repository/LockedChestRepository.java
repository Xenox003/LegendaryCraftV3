package de.jxdev.legendarycraft.v3.data.repository;

import de.jxdev.legendarycraft.v3.data.db.IDatabaseService;
import de.jxdev.legendarycraft.v3.data.models.BlockPos;
import de.jxdev.legendarycraft.v3.data.models.LockedChest;
import org.bukkit.Location;
import org.bukkit.block.Block;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class LockedChestRepository {
    private final IDatabaseService db;
    public LockedChestRepository(IDatabaseService db) {
        this.db = db;
    }

    private LockedChest mapChest(ResultSet rs) throws SQLException {
        LockedChest chest = new LockedChest();
        chest.setTeamId(rs.getInt("team_id"));
        UUID worldId = UUID.fromString(rs.getString("world_id"));
        int x = rs.getInt("x");
        int y = rs.getInt("y");
        int z = rs.getInt("z");
        chest.setBlockPos(new BlockPos(worldId, x, y, z));
        return chest;
    }

    public Optional<LockedChest> find(BlockPos pos) throws SQLException {
        return db.queryOne(
                "SELECT team_id,world_id,x,y,z FROM locked_chests WHERE x=? AND y=? and z=? AND world_id=?",
                this::mapChest,
                pos.x(),
                pos.y(),
                pos.z(),
                pos.worldId().toString()
        );
    }

    public List<LockedChest> findByTeam(int teamId) throws SQLException {
        return db.queryList("SELECT team_id,world_id,x,y,z FROM locked_chests WHERE team_id=?", this::mapChest, teamId);
    }
    public List<LockedChest> findAll() throws SQLException {
        return db.queryList("SELECT team_id,world_id,x,y,z FROM locked_chests", this::mapChest);
    }

    public void create(int teamId, BlockPos pos) throws SQLException {
        db.update("INSERT INTO locked_chests(team_id,world_id,x,y,z) VALUES(?,?,?,?,?)", teamId, pos.worldId().toString(), pos.x(), pos.y(), pos.z());
    }
    public void delete(LockedChest chest) throws SQLException {
        db.update("DELETE FROM locked_chests WHERE x=? AND y=? and z=? AND world_id=?", chest.getBlockPos().x(), chest.getBlockPos().y(), chest.getBlockPos().z(), chest.getBlockPos().worldId().toString());
    }
}
