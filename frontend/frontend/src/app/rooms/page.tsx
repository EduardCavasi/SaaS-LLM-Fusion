'use client';

import { useEffect, useState } from 'react';
import { roomApi } from '@/lib/api';
import { Room } from '@/types';

export default function RoomsPage() {
  const [rooms, setRooms] = useState<Room[]>([]);
  const [loading, setLoading] = useState(true);
  const [showModal, setShowModal] = useState(false);
  const [editingRoom, setEditingRoom] = useState<Room | null>(null);
  const [formData, setFormData] = useState({
    name: '',
    capacity: 1,
    location: '',
    description: '',
    available: true,
  });

  const fetchRooms = async () => {
    try {
      const data = await roomApi.getAll();
      setRooms(data);
    } catch (error) {
      console.error('Failed to fetch rooms:', error);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchRooms();
  }, []);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      if (editingRoom) {
        await roomApi.update(editingRoom.id, formData);
      } else {
        await roomApi.create(formData);
      }
      fetchRooms();
      closeModal();
    } catch (error) {
      console.error('Failed to save room:', error);
    }
  };

  const handleEdit = (room: Room) => {
    setEditingRoom(room);
    setFormData({
      name: room.name,
      capacity: room.capacity,
      location: room.location || '',
      description: room.description || '',
      available: room.available,
    });
    setShowModal(true);
  };

  const handleDelete = async (id: number) => {
    if (!confirm('Are you sure you want to delete this room?')) return;
    try {
      await roomApi.delete(id);
      fetchRooms();
    } catch (error) {
      console.error('Failed to delete room:', error);
    }
  };

  const closeModal = () => {
    setShowModal(false);
    setEditingRoom(null);
    setFormData({
      name: '',
      capacity: 1,
      location: '',
      description: '',
      available: true,
    });
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="text-[var(--muted-foreground)]">Loading rooms...</div>
      </div>
    );
  }

  return (
    <div className="space-y-6 animate-fade-in">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold">Rooms</h1>
          <p className="text-[var(--muted-foreground)]">
            Manage meeting rooms and their capacities
          </p>
        </div>
        <button onClick={() => setShowModal(true)} className="btn btn-primary">
          + Add Room
        </button>
      </div>

      {/* Rooms Grid */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
        {rooms.map((room) => (
          <div key={room.id} className="card">
            <div className="flex items-start justify-between mb-3">
              <div>
                <h3 className="font-semibold text-lg">{room.name}</h3>
                {room.location && (
                  <p className="text-sm text-[var(--muted-foreground)]">
                    📍 {room.location}
                  </p>
                )}
              </div>
              <span className={`badge ${room.available ? 'badge-confirmed' : 'badge-rejected'}`}>
                {room.available ? 'Available' : 'Unavailable'}
              </span>
            </div>

            <div className="flex items-center gap-4 mb-3">
              <div className="flex items-center gap-2">
                <span className="text-2xl">👥</span>
                <div>
                  <div className="font-bold text-xl">{room.capacity}</div>
                  <div className="text-xs text-[var(--muted-foreground)]">Capacity</div>
                </div>
              </div>
            </div>

            {room.description && (
              <p className="text-sm text-[var(--muted-foreground)] mb-4">
                {room.description}
              </p>
            )}

            <div className="flex gap-2 pt-3 border-t border-[var(--border)]">
              <button
                onClick={() => handleEdit(room)}
                className="btn btn-secondary flex-1 text-sm"
              >
                Edit
              </button>
              <button
                onClick={() => handleDelete(room.id)}
                className="btn btn-danger text-sm"
              >
                Delete
              </button>
            </div>
          </div>
        ))}

        {rooms.length === 0 && (
          <div className="col-span-full text-center py-12 text-[var(--muted-foreground)]">
            <div className="text-4xl mb-2">🏢</div>
            <p>No rooms available</p>
            <p className="text-sm">Add your first room to get started</p>
          </div>
        )}
      </div>

      {/* Add/Edit Room Modal */}
      {showModal && (
        <div className="modal-overlay" onClick={closeModal}>
          <div className="modal-content" onClick={(e) => e.stopPropagation()}>
            <h2 className="text-xl font-bold mb-4">
              {editingRoom ? 'Edit Room' : 'Add New Room'}
            </h2>

            <form onSubmit={handleSubmit} className="space-y-4">
              <div>
                <label className="block text-sm font-medium mb-1">Room Name *</label>
                <input
                  type="text"
                  className="input"
                  value={formData.name}
                  onChange={(e) => setFormData({ ...formData, name: e.target.value })}
                  required
                />
              </div>

              <div>
                <label className="block text-sm font-medium mb-1">Capacity *</label>
                <input
                  type="number"
                  className="input"
                  min={1}
                  value={formData.capacity}
                  onChange={(e) => setFormData({ ...formData, capacity: Number(e.target.value) })}
                  required
                />
                <p className="text-xs text-[var(--muted-foreground)] mt-1">
                  Maximum number of participants (used by Z3 constraint solver)
                </p>
              </div>

              <div>
                <label className="block text-sm font-medium mb-1">Location</label>
                <input
                  type="text"
                  className="input"
                  value={formData.location}
                  onChange={(e) => setFormData({ ...formData, location: e.target.value })}
                  placeholder="e.g., Building 1, Floor 2"
                />
              </div>

              <div>
                <label className="block text-sm font-medium mb-1">Description</label>
                <textarea
                  className="input"
                  rows={2}
                  value={formData.description}
                  onChange={(e) => setFormData({ ...formData, description: e.target.value })}
                  placeholder="Room features and equipment..."
                />
              </div>

              <div>
                <label className="flex items-center gap-2 cursor-pointer">
                  <input
                    type="checkbox"
                    checked={formData.available}
                    onChange={(e) => setFormData({ ...formData, available: e.target.checked })}
                    className="rounded"
                  />
                  <span className="text-sm">Room is available for booking</span>
                </label>
              </div>

              <div className="flex gap-3 pt-4">
                <button type="submit" className="btn btn-primary flex-1">
                  {editingRoom ? 'Save Changes' : 'Add Room'}
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

