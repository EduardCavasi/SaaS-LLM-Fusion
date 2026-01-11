'use client';

import { useEffect, useState } from 'react';
import { participantApi } from '@/lib/api';
import { Participant } from '@/types';

export default function ParticipantsPage() {
  const [participants, setParticipants] = useState<Participant[]>([]);
  const [loading, setLoading] = useState(true);
  const [showModal, setShowModal] = useState(false);
  const [editingParticipant, setEditingParticipant] = useState<Participant | null>(null);
  const [formData, setFormData] = useState({
    name: '',
    email: '',
    department: '',
  });

  const fetchParticipants = async () => {
    try {
      const data = await participantApi.getAll();
      setParticipants(data);
    } catch (error) {
      console.error('Failed to fetch participants:', error);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchParticipants();
  }, []);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      if (editingParticipant) {
        await participantApi.update(editingParticipant.id, formData);
      } else {
        await participantApi.create(formData);
      }
      fetchParticipants();
      closeModal();
    } catch (error) {
      console.error('Failed to save participant:', error);
    }
  };

  const handleEdit = (participant: Participant) => {
    setEditingParticipant(participant);
    setFormData({
      name: participant.name,
      email: participant.email,
      department: participant.department || '',
    });
    setShowModal(true);
  };

  const handleDelete = async (id: number) => {
    if (!confirm('Are you sure you want to delete this participant?')) return;
    try {
      await participantApi.delete(id);
      fetchParticipants();
    } catch (error) {
      console.error('Failed to delete participant:', error);
    }
  };

  const closeModal = () => {
    setShowModal(false);
    setEditingParticipant(null);
    setFormData({
      name: '',
      email: '',
      department: '',
    });
  };

  // Group participants by department
  const groupedParticipants = participants.reduce((acc, p) => {
    const dept = p.department || 'Other';
    if (!acc[dept]) acc[dept] = [];
    acc[dept].push(p);
    return acc;
  }, {} as Record<string, Participant[]>);

  if (loading) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="text-[var(--muted-foreground)]">Loading participants...</div>
      </div>
    );
  }

  return (
    <div className="space-y-6 animate-fade-in">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold">Participants</h1>
          <p className="text-[var(--muted-foreground)]">
            Manage users who can attend meetings
          </p>
        </div>
        <button onClick={() => setShowModal(true)} className="btn btn-primary">
          + Add Participant
        </button>
      </div>

      {/* Stats */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
        <div className="stat-card">
          <div className="text-[var(--muted-foreground)] text-sm mb-1">Total Participants</div>
          <div className="stat-value">{participants.length}</div>
        </div>
        <div className="stat-card">
          <div className="text-[var(--muted-foreground)] text-sm mb-1">Departments</div>
          <div className="stat-value">{Object.keys(groupedParticipants).length}</div>
        </div>
        <div className="stat-card">
          <div className="text-[var(--muted-foreground)] text-sm mb-1">Most Active Dept</div>
          <div className="text-xl font-bold text-[var(--primary)]">
            {Object.entries(groupedParticipants).sort((a, b) => b[1].length - a[1].length)[0]?.[0] || 'N/A'}
          </div>
        </div>
      </div>

      {/* Participants by Department */}
      {Object.entries(groupedParticipants).map(([dept, members]) => (
        <div key={dept} className="card">
          <div className="flex items-center justify-between mb-4">
            <h2 className="text-lg font-semibold flex items-center gap-2">
              <span className="text-xl">🏷️</span>
              {dept}
              <span className="text-sm font-normal text-[var(--muted-foreground)]">
                ({members.length})
              </span>
            </h2>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-3">
            {members.map((participant) => (
              <div
                key={participant.id}
                className="flex items-center justify-between p-3 bg-[var(--secondary)] rounded-lg"
              >
                <div className="flex items-center gap-3">
                  <div className="w-10 h-10 rounded-full bg-[var(--primary)] flex items-center justify-center text-white font-medium">
                    {participant.name.split(' ').map(n => n[0]).join('').slice(0, 2)}
                  </div>
                  <div>
                    <div className="font-medium">{participant.name}</div>
                    <div className="text-xs text-[var(--muted-foreground)]">
                      {participant.email}
                    </div>
                  </div>
                </div>
                <div className="flex gap-1">
                  <button
                    onClick={() => handleEdit(participant)}
                    className="p-2 hover:bg-[var(--muted)] rounded transition-colors"
                    title="Edit"
                  >
                    ✏️
                  </button>
                  <button
                    onClick={() => handleDelete(participant.id)}
                    className="p-2 hover:bg-red-900/30 rounded transition-colors"
                    title="Delete"
                  >
                    🗑️
                  </button>
                </div>
              </div>
            ))}
          </div>
        </div>
      ))}

      {participants.length === 0 && (
        <div className="card text-center py-12">
          <div className="text-4xl mb-2">👥</div>
          <p className="text-[var(--muted-foreground)]">No participants yet</p>
          <p className="text-sm text-[var(--muted-foreground)]">
            Add participants to start scheduling meetings
          </p>
        </div>
      )}

      {/* Add/Edit Participant Modal */}
      {showModal && (
        <div className="modal-overlay" onClick={closeModal}>
          <div className="modal-content" onClick={(e) => e.stopPropagation()}>
            <h2 className="text-xl font-bold mb-4">
              {editingParticipant ? 'Edit Participant' : 'Add New Participant'}
            </h2>

            <form onSubmit={handleSubmit} className="space-y-4">
              <div>
                <label className="block text-sm font-medium mb-1">Name *</label>
                <input
                  type="text"
                  className="input"
                  value={formData.name}
                  onChange={(e) => setFormData({ ...formData, name: e.target.value })}
                  required
                />
              </div>

              <div>
                <label className="block text-sm font-medium mb-1">Email *</label>
                <input
                  type="email"
                  className="input"
                  value={formData.email}
                  onChange={(e) => setFormData({ ...formData, email: e.target.value })}
                  required
                />
                <p className="text-xs text-[var(--muted-foreground)] mt-1">
                  Used for participant conflict detection by Z3 solver
                </p>
              </div>

              <div>
                <label className="block text-sm font-medium mb-1">Department</label>
                <input
                  type="text"
                  className="input"
                  value={formData.department}
                  onChange={(e) => setFormData({ ...formData, department: e.target.value })}
                  placeholder="e.g., Engineering, Marketing, HR"
                />
              </div>

              <div className="flex gap-3 pt-4">
                <button type="submit" className="btn btn-primary flex-1">
                  {editingParticipant ? 'Save Changes' : 'Add Participant'}
                </button>
                <button type="button" onClick={closeModal} className="btn btn-secondary">
                  Cancel
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}

