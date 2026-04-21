import { useEffect, useState } from "react";
import api from "../api/api";
import {
  DndContext,
  closestCenter
} from "@dnd-kit/core";

import {
  SortableContext,
  verticalListSortingStrategy,
  useSortable,
  arrayMove
} from "@dnd-kit/sortable";

import { CSS } from "@dnd-kit/utilities";

function TodosPage({ keycloak }) {
  const [user, setUser] = useState(null);
  const [todos, setTodos] = useState([]);
  const [newTodo, setNewTodo] = useState("");
  const [editingId, setEditingId] = useState(null);
  const [editingText, setEditingText] = useState("");

  const client = api(keycloak);

  useEffect(() => {
    fetchUser();
    fetchTodos();
  }, []);

  const fetchUser = async () => {
    try {
      const res = await client.get("/api/me");
      setUser(res.data);
    } catch (e) {
      console.error("Ошибка пользователя", e);
    }
  };

  const fetchTodos = async () => {
    try {
      const res = await client.get("/api/todos");
      setTodos(res.data);
    } catch (e) {
      console.error("Ошибка загрузки todo", e);
    }
  };

  const createTodo = async () => {
    if (!newTodo.trim()) return;

    try {
      await client.post("/api/todos", {
        title: newTodo
      });

      setNewTodo("");
      fetchTodos();
    } catch (e) {
      console.error("Ошибка создания todo", e);
    }
  };

  const toggleTodo = async (todo) => {
    try {
      await client.put(`/api/todos/${todo.id}`, {
        title: todo.title,
        completed: !todo.completed
      });

      fetchTodos();
    } catch (e) {
      console.error("Ошибка обновления todo", e);
    }
  };

  const deleteTodo = async (id) => {
    try {
      await client.delete(`/api/todos/${id}`);
      fetchTodos();
    } catch (e) {
      console.error("Ошибка удаления todo", e);
    }
  };

  const updateTodo = async (todo) => {
    try {
      await client.put(`/api/todos/${todo.id}`, {
        title: editingText,
        completed: todo.completed
      });

      setEditingId(null);
      setEditingText("");
      fetchTodos();
    } catch (e) {
      console.error("Ошибка редактирования", e);
    }
  };

  const handleDragEnd = async (event) => {
    const { active, over } = event;

    if (!over || active.id === over.id) return;

    const oldIndex = todos.findIndex((t) => t.id === active.id);
    const newIndex = todos.findIndex((t) => t.id === over.id);

    const newTodos = arrayMove(todos, oldIndex, newIndex);
    setTodos(newTodos);

    try {
      await client.put(`/api/todos/${active.id}/reorder`, {
        newPosition: newIndex
      });
    } catch (e) {
      console.error("Ошибка reorder", e);
    }
  };

return (
  <div style={{ padding: 20 }}>
    <h2>My Todos</h2>

    {user && (
      <div style={{ marginBottom: 20 }}>
        <b>{user.username}</b> ({user.email})
      </div>
    )}

    {/* Создание задачи */}
    <div style={{ marginBottom: 20 }}>
      <input
        value={newTodo}
        onChange={(e) => setNewTodo(e.target.value)}
        placeholder="Новая задача..."
        style={styles.input}
      />
      <button onClick={createTodo} style={styles.addBtn}>
        Добавить
      </button>
    </div>

    {/* Список задач */}
    <DndContext collisionDetection={closestCenter} onDragEnd={handleDragEnd}>
      <SortableContext
        items={todos.map((t) => t.id)}
        strategy={verticalListSortingStrategy}
      >
        <div>
          {todos.length === 0 ? (
            <p>Нет задач</p>
          ) : (
            todos.map((todo) => (
              <SortableItem key={todo.id} todo={todo}>
                {({ attributes, listeners }) => (
                  <div style={styles.todoRow}>

                    {/* drag handle */}
                    <div {...attributes} {...listeners} style={styles.dragHandle}>
                      ☰
                    </div>

                    <div style={styles.left}>
                      <input
                        type="checkbox"
                        checked={todo.completed}
                        onChange={() => toggleTodo(todo)}
                      />

                      {editingId === todo.id ? (
                        <input
                          value={editingText}
                          onChange={(e) => setEditingText(e.target.value)}
                          onBlur={() => updateTodo(todo)}
                          onKeyDown={(e) => {
                            if (e.key === "Enter") updateTodo(todo);
                          }}
                          autoFocus
                          style={styles.input}
                        />
                      ) : (
                        <span
                            style={{
                              cursor: "pointer",
                              textDecoration: todo.completed ? "line-through" : "none",
                              overflow: "hidden",
                              textOverflow: "ellipsis",
                              whiteSpace: "nowrap"
                            }}
                          onClick={() => {
                            setEditingId(todo.id);
                            setEditingText(todo.title);
                          }}
                          style={{
                            marginLeft: 10,
                            cursor: "pointer",
                            textDecoration: todo.completed
                              ? "line-through"
                              : "none"
                          }}
                        >
                          {todo.title}
                        </span>
                      )}
                    </div>

                    <button
                      style={styles.deleteBtn}
                      onClick={() => deleteTodo(todo.id)}
                    >
                      ❌
                    </button>

                  </div>
                )}
              </SortableItem>
            ))
          )}
        </div>
      </SortableContext>
    </DndContext>
  </div>
);
}

const styles = {
  todo: {
    padding: "10px",
    marginBottom: "10px",
    border: "1px solid #ccc",
    borderRadius: "6px"
  },

  input: {
    padding: "8px",
    marginRight: "10px",
    borderRadius: "6px",
    border: "1px solid #ccc"
  },

  addBtn: {
    padding: "8px 12px",
    background: "#4CAF50",
    color: "white",
    border: "none",
    borderRadius: "6px",
    cursor: "pointer"
  },

todoRow: {
  display: "flex",
  alignItems: "center",
  gap: "10px",
  padding: "10px",
  marginBottom: "10px",
  border: "1px solid #ccc",
  borderRadius: "6px"
},

left: {
  display: "flex",
  alignItems: "center",
  gap: "10px",
  flex: 1,
  minWidth: 0
},

  deleteBtn: {
    background: "transparent",
    border: "none",
    cursor: "pointer",
    fontSize: "16px"
  },

  dragHandle: {
    cursor: "grab",
    marginRight: "10px",
    padding: "5px"
  }

};

function SortableItem({ todo, children }) {
  const {
    attributes,
    listeners,
    setNodeRef,
    transform,
    transition
  } = useSortable({ id: todo.id });

  const style = {
    transform: CSS.Transform.toString(transform),
    transition
  };

  return (
    <div ref={setNodeRef} style={style}>
      {children({ attributes, listeners })}
    </div>
  );
}

export default TodosPage;