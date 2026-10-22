package com.vergiliusproject.api;

import com.vergiliusproject.entities.Os;
import com.vergiliusproject.entities.Tdata;
import com.vergiliusproject.entities.Ttype;
import com.vergiliusproject.repos.TtypeRepository;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * Bulk loader resolving the full type graph reachable from a root type into a
 * Map keyed by YAML type id, using chunked IN queries instead of per-node
 * lookups. All consumers of Ttype.getData() must pass defensive copies of the
 * set to anything FieldBuilder-derived: getFirstPieceOfStruct mutates its
 * input list.
 */
@Component
public class TypeLoader {

    static final int CHUNK_SIZE = 500;
    static final int MAX_NODES = 100_000;

    private final TtypeRepository repo;

    public TypeLoader(TtypeRepository repo) {
        this.repo = repo;
    }

    /**
     * Loads the root type and the transitive closure of types it references,
     * bounded by MAX_NODES as a hard safety cap. Returns YAML type id -> Ttype.
     */
    public Map<Integer, Ttype> loadClosure(Os os, Integer rootId) {
        Map<Integer, Ttype> closure = new HashMap<>();
        Set<Integer> visited = new HashSet<>();
        Deque<Integer> frontier = new ArrayDeque<>();

        if (rootId != null) {
            frontier.add(rootId);
        }

        while (!frontier.isEmpty() && closure.size() < MAX_NODES) {
            List<Integer> batch = new ArrayList<>(CHUNK_SIZE);
            while (!frontier.isEmpty() && batch.size() < CHUNK_SIZE) {
                int id = frontier.poll();
                if (visited.add(id)) {
                    batch.add(id);
                }
            }
            if (batch.isEmpty()) {
                continue;
            }

            for (Ttype t : repo.findByIdInAndOpersys(batch, os)) {
                closure.put(t.getId(), t);
            }

            for (int id : batch) {
                Ttype t = closure.get(id);
                if (t != null) {
                    collectRefs(t, visited, frontier);
                }
            }
        }

        return closure;
    }

    private void collectRefs(Ttype t, Set<Integer> visited, Deque<Integer> frontier) {
        if (t.getData() == null) {
            return;
        }
        for (Tdata d : t.getData()) {
            Integer ref = d.getId();
            if (ref != null && !visited.contains(ref)) {
                frontier.add(ref);
            }
        }
    }

    /** Defensive copy: FieldBuilder's getFirstPieceOfStruct mutates its input list. */
    public static List<Tdata> fieldsOf(Ttype t) {
        if (t.getData() == null) {
            return List.of();
        }
        return new ArrayList<>(t.getData());
    }

    /** Sorted-by-ordinal defensive copy, matching Sorter.sortByOrdinal output. */
    public static List<Tdata> fieldsSortedByOrdinal(Ttype t) {
        List<Tdata> fields = fieldsOf(t);
        fields.sort(java.util.Comparator.comparing(Tdata::getOrdinal,
                java.util.Comparator.nullsLast(java.util.Comparator.naturalOrder())));
        return fields;
    }

    public Ttype loadOne(Os os, Integer id) {
        Collection<Integer> ids = List.of(id);
        List<Ttype> found = repo.findByIdInAndOpersys(ids, os);
        return found.isEmpty() ? null : found.getFirst();
    }
}
