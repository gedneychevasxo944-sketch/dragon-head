package org.dragon.workspace.member;

import org.dragon.store.StoreType;
import org.dragon.store.StoreTypeAnn;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * TeamPosition 内存存储实现
 *
 * @author qieqie
 * @version 1.0
 */
@Component
@StoreTypeAnn(StoreType.MEMORY)
public class MemoryTeamPositionStore implements TeamPositionStore {

    private final Map<String, TeamPosition> positions = new ConcurrentHashMap<>();

    @Override
    public void save(TeamPosition position) {
        positions.put(position.getId(), position);
    }

    @Override
    public void update(TeamPosition position) {
        positions.put(position.getId(), position);
    }

    @Override
    public void delete(String id) {
        positions.remove(id);
    }

    @Override
    public Optional<TeamPosition> findById(String id) {
        return Optional.ofNullable(positions.get(id));
    }

    @Override
    public List<TeamPosition> findByWorkspaceId(String workspaceId) {
        return positions.values().stream()
                .filter(pos -> workspaceId.equals(pos.getWorkspaceId()))
                .collect(Collectors.toList());
    }

    @Override
    public Optional<TeamPosition> findByWorkspaceIdAndRoleName(String workspaceId, String roleName) {
        return positions.values().stream()
                .filter(pos -> workspaceId.equals(pos.getWorkspaceId())
                        && roleName.equals(pos.getRoleName()))
                .findFirst();
    }
}
