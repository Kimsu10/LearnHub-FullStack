import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { FaCalendarPlus } from 'react-icons/fa';
import './Calendar.css';
import Modal from './Modal';

const Calendar = () => {
  const [currentDate, setCurrentDate] = useState(new Date());
  const [selectedDate, setSelectedDate] = useState(null);
  const [events, setEvents] = useState([]);
  const [newEvent, setNewEvent] = useState({ title: '', start: null, end: null, color: '#FF9999', content: '' });
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [viewEvent, setViewEvent] = useState(null);
  const [editEvent, setEditEvent] = useState(null);

  // 컴포넌트가 마운트될 때 로컬 스토리지에서 일정 불러오기
  useEffect(() => {
    const storedEvents = JSON.parse(localStorage.getItem('calendarEvents')) || [];
    setEvents(storedEvents);
  }, []);

  // 일정이 변경될 때 로컬 스토리지에 저장
  useEffect(() => {
    localStorage.setItem('calendarEvents', JSON.stringify(events));
  }, [events]);

  // 해당 월의 날짜 수 계산
  const daysInMonth = new Date(currentDate.getFullYear(), currentDate.getMonth() + 1, 0).getDate();
  // 해당 월의 첫 번째 날짜 요일 계산
  const firstDayOfMonth = new Date(currentDate.getFullYear(), currentDate.getMonth(), 1).getDay();

  // 달력 날짜 생성
  const generateCalendarDates = () => {
    const dates = [];
    const prevMonthLastDate = new Date(currentDate.getFullYear(), currentDate.getMonth(), 0);
    const nextMonthFirstDate = new Date(currentDate.getFullYear(), currentDate.getMonth() + 1, 1);

    // 이전 달 날짜 추가
    for (let i = firstDayOfMonth - 1; i >= 0; i--) {
      dates.push({
        date: new Date(prevMonthLastDate.getFullYear(), prevMonthLastDate.getMonth(), prevMonthLastDate.getDate() - i),
        isCurrentMonth: false
      });
    }

    // 현재 달 날짜 추가
    for (let i = 1; i <= daysInMonth; i++) {
      dates.push({
        date: new Date(currentDate.getFullYear(), currentDate.getMonth(), i),
        isCurrentMonth: true
      });
    }

    // 다음 달 날짜 추가
    const remainingDays = 7 - (dates.length % 7);
    if (remainingDays < 7) {
      for (let i = 0; i < remainingDays; i++) {
        dates.push({
          date: new Date(nextMonthFirstDate.getFullYear(), nextMonthFirstDate.getMonth(), i + 1),
          isCurrentMonth: false
        });
      }
    }

    return dates;
  };

  // 월 변경
  const handleMonthChange = (direction) => {
    setCurrentDate(new Date(currentDate.getFullYear(), currentDate.getMonth() + direction, 1));
  };

  const handleOpenModal = () => {
    setNewEvent({ title: '', start: selectedDate, end: selectedDate, color: '#FF9999', content: '' });
    setIsModalOpen(true);
  };

  const handleCloseModal = () => {
    setIsModalOpen(false);
    setNewEvent({ title: '', start: null, end: null, color: '#FF9999', content: '' });
    setViewEvent(null);
    setEditEvent(null);
  };

  // 일정 저장
  const handleSaveEvent = () => {
    if (newEvent.title && newEvent.start && newEvent.end) {
      if (editEvent) {
        setEvents(events.map(event => (event.id === editEvent.id ? newEvent : event)));
      } else {
        setEvents([...events, { ...newEvent, id: Date.now() }]);
      }
      handleCloseModal();
    }
  };

  // 일정 삭제
  const handleDeleteEvent = () => {
    setEvents(events.filter(event => event.id !== viewEvent.id));
    handleCloseModal();
  };

  // 날짜 클릭
  const handleDateClick = (date) => {
    const adjustedDate = new Date(Date.UTC(date.getFullYear(), date.getMonth(), date.getDate()));
    setSelectedDate(adjustedDate);
  };

  // 특정 날짜 일정
  const getEventsForDate = (date) => {
    return events.filter(event =>
      new Date(event.start).toDateString() === date.toDateString() ||
      (new Date(event.start) <= date && new Date(event.end) >= date)
    );
  };

  // 현재 날짜 확인
  const isCurrentDate = (date) => {
    const today = new Date();
    return date && date.getDate() === today.getDate() &&
           date.getMonth() === today.getMonth() &&
           date.getFullYear() === today.getFullYear();
  };

  // 일정 스타일 설정
  const getEventStyle = (event, day) => {
    const startDate = new Date(event.start);
    const endDate = new Date(event.end);
    const span = (endDate - startDate) / (1000 * 60 * 60 * 24) + 1;
    if (startDate.toDateString() === day.toDateString()) {
      if (startDate.toDateString() !== endDate.toDateString()) {
        return { gridColumn: `span ${span}` };
      }
    }
    return {};
  };

  return (
    <section className="calendar-container">
      <div className="calendar">
        <div className="calendar-header">
          <button onClick={() => handleMonthChange(-1)}>&lt;</button>
          <h2>{currentDate.getFullYear()}년 {currentDate.getMonth() + 1}월</h2>
          <button onClick={() => handleMonthChange(1)}>&gt;</button>
        </div>
        <div className="add-list">
          <button onClick={() => setCurrentDate(new Date())}>
            Today
          </button>
          <button onClick={handleOpenModal}>
            일정 추가
            <FaCalendarPlus style={{ marginLeft: '8px' }} />
          </button>
        </div>
        <div className="calendar-body">
          <div className="weekdays">
            {['일', '월', '화', '수', '목', '금', '토'].map(day => (
              <div key={day}>{day}</div>
            ))}
          </div>
          <div className="days">
            {generateCalendarDates().map((day, index) => (
              <div
                key={index}
                className={`day ${day.isCurrentMonth ? '' : 'other-month'} ${
                  selectedDate &&
                  day.date.getFullYear() === selectedDate.getFullYear() &&
                  day.date.getMonth() === selectedDate.getMonth() &&
                  day.date.getDate() === selectedDate.getDate() ? 'selected' : ''
                } ${isCurrentDate(day.date) ? 'current-date' : ''}`}
                onClick={() => handleDateClick(day.date)}
              >
                <span className="day-number">{day.date.getDate()}</span>
                <div className="events-indicator">
                  {getEventsForDate(day.date).slice(0, 2).map(event => (
                    <Link key={event.id} to={`/managers/calendar/${event.id}`} className="event-bar" style={{ backgroundColor: event.color, ...getEventStyle(event, day.date) }}>
                      <span className="event-title">{event.title}</span>
                    </Link>
                  ))}
                </div>
              </div>
            ))}
          </div>
        </div>
      </div>

      <Modal isOpen={isModalOpen} onClose={handleCloseModal}>
        <div>
          <h3 className="calendar-modal-add">{editEvent ? '일정 수정하기' : '일정 등록하기'}</h3>
          <div className="calendar-event-form-edit">
            <label>제목</label>
            <input
              type="text"
              placeholder="일정 제목"
              value={newEvent.title}
              onChange={(e) => setNewEvent({ ...newEvent, title: e.target.value })}
            />
            <div className="date-input-container">
              <label>시작일</label>
              <input
                type="date"
                value={newEvent.start ? newEvent.start.toISOString().substr(0, 10) : ''}
                onChange={(e) => setNewEvent({ ...newEvent, start: new Date(e.target.value) })}
              />
            </div>
            <div className="date-input-container">
              <label>종료일</label>
              <input
                type="date"
                value={newEvent.end ? newEvent.end.toISOString().substr(0, 10) : ''}
                onChange={(e) => setNewEvent({ ...newEvent, end: new Date(e.target.value) })}
              />
            </div>
            <div className="color-picker">
              {['#FF9999', '#99FF99', '#9999FF'].map(color => (
                <div
                  key={color}
                  className={`color-option ${newEvent.color === color ? 'selected' : ''}`}
                  style={{ backgroundColor: color }}
                  onClick={() => setNewEvent({ ...newEvent, color })}
                ></div>
              ))}
            </div>
            <div className='calendar-submit'>
              <button onClick={handleSaveEvent}>{editEvent ? '수정' : '등록'}</button>
              {editEvent && <button onClick={handleDeleteEvent}>삭제</button>}
            </div>
          </div>
        </div>
      </Modal>
    </section>
  );
};

export default Calendar;
