package net.minespree.dominion.player.parties;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import lombok.Value;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PartyInviteManager {
    public static final long PARTY_INVITES_EXPIRE_WHEN = 60 * 1000;
    private final Map<UUID, Set<Invite>> invitesByParty = new ConcurrentHashMap<>();

    public int getRemainingInvites(Party party) {
        Set<Invite> invites = invitesByParty.getOrDefault(party.getOwner(), ImmutableSet.of());
        int outstandingUnexpired = (int) invites.stream().filter(i -> i.expires >= System.currentTimeMillis()).count();
        return PartyManager.MEMBER_CAP - outstandingUnexpired - party.getMembers().size();
    }

    public int getRemainingInvites(UUID owner) {
        Set<Invite> invites = invitesByParty.getOrDefault(owner, ImmutableSet.of());
        int outstandingUnexpired = (int) invites.stream().filter(i -> i.expires >= System.currentTimeMillis()).count();
        return PartyManager.MEMBER_CAP - outstandingUnexpired;
    }

    public boolean addInvite(UUID inviter, UUID invited) {
        Set<Invite> invites = invitesByParty.computeIfAbsent(inviter, (k) -> Sets.newConcurrentHashSet());
        return invites.add(new Invite(inviter, invited, System.currentTimeMillis() + PARTY_INVITES_EXPIRE_WHEN));
    }

    public boolean removeInvite(UUID inviter, UUID invited) {
        Set<Invite> list = invitesByParty.get(inviter);
        if (list == null) return false;
        return list.removeIf(i -> i.to.equals(invited));
    }

    public boolean wasInvited(UUID inviter, UUID invited) {
        Set<Invite> invites = invitesByParty.getOrDefault(inviter, ImmutableSet.of());
        return invites.stream().anyMatch(i -> i.to.equals(invited) && i.expires >= System.currentTimeMillis());
    }

    public void remove(UUID inviter) {
        invitesByParty.remove(inviter);
    }

    public void removeAllInvites(UUID actor) {
        for (Set<Invite> uuids : invitesByParty.values()) {
            uuids.removeIf(i -> i.to.equals(actor));
        }
    }

    public void pruneExpired() {
        for (Set<Invite> uuids : invitesByParty.values()) {
            uuids.removeIf(i -> i.expires < System.currentTimeMillis());
        }
    }

    @Value
    public static class Invite {
        private final UUID from;
        private final UUID to;
        private final long expires;

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            if (!super.equals(o)) return false;

            Invite invite = (Invite) o;

            if (!from.equals(invite.from)) return false;
            return to.equals(invite.to);
        }

        @Override
        public int hashCode() {
            int result = super.hashCode();
            result = 31 * result + from.hashCode();
            result = 31 * result + to.hashCode();
            return result;
        }
    }
}
