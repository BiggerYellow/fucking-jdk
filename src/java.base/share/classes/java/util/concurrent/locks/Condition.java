/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

/*
 * This file is available under and governed by the GNU General Public
 * License version 2 only, as published by the Free Software Foundation.
 * However, the following notice accompanied the original version of this
 * file:
 *
 * Written by Doug Lea with assistance from members of JCP JSR-166
 * Expert Group and released to the public domain, as explained at
 * http://creativecommons.org/publicdomain/zero/1.0/
 */

package java.util.concurrent.locks;

import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * {@code Condition} factors out the {@code Object} monitor
 * methods ({@link Object#wait() wait}, {@link Object#notify notify}
 * and {@link Object#notifyAll notifyAll}) into distinct objects to
 * give the effect of having multiple wait-sets per object, by
 * combining them with the use of arbitrary {@link Lock} implementations.
 * Where a {@code Lock} replaces the use of {@code synchronized} methods
 * and statements, a {@code Condition} replaces the use of the Object
 * monitor methods.
 * Condition将对象监视器方法(wait、notify、notifyAll)分解为不同的对象，通过将每个对象与任意Lock实现相结合，达到每个对象具有多等待集的效果。
 * lock代替使用synchronize方法和语句的使用，Condition取代了Object监视器方法的使用。
 *
 * <p>Conditions (also known as <em>condition queues</em> or
 * <em>condition variables</em>) provide a means for one thread to
 * suspend execution (to &quot;wait&quot;) until notified by another
 * thread that some state condition may now be true.  Because access
 * to this shared state information occurs in different threads, it
 * must be protected, so a lock of some form is associated with the
 * condition. The key property that waiting for a condition provides
 * is that it <em>atomically</em> releases the associated lock and
 * suspends the current thread, just like {@code Object.wait}.
 *  条件（又被称为条件队列或条件队列）为线程提供一种暂停线程的方法，直到被其他线程通知一些状态条件现在可能是true。
 * 因为对这个共享状态信息的访问发生在不同线程中，所以它必须是受保护的，
 *
 * <p>A {@code Condition} instance is intrinsically bound to a lock.
 * To obtain a {@code Condition} instance for a particular {@link Lock}
 * instance use its {@link Lock#newCondition newCondition()} method.
 * Condition实例本质上绑定了锁。
 * 要获取特定锁的示例，要通过调用Lock.newCondition方法
 *
 * <p>As an example, suppose we have a bounded buffer which supports
 * {@code put} and {@code take} methods.  If a
 * {@code take} is attempted on an empty buffer, then the thread will block
 * until an item becomes available; if a {@code put} is attempted on a
 * full buffer, then the thread will block until a space becomes available.
 * We would like to keep waiting {@code put} threads and {@code take}
 * threads in separate wait-sets so that we can use the optimization of
 * only notifying a single thread at a time when items or spaces become
 * available in the buffer. This can be achieved using two
 * {@link Condition} instances.
 * 例如，假设我们拥有一个支持put和take方法的有界缓冲区。
 * 如果尝试在空缓冲区中执行take操作，则线程将阻塞直到缓冲区可用
 * 如果尝试在满缓冲区执行put操作，线程将阻塞直到空间可用。
 * 我们希望put线程和take线程在不同的等待集中保持等待，这样当缓存中的空间可用时我们可以一次只通知一个线程的优化。
 * 可以通过使用两个Condition实例完成
 *
 * <pre>
 * class BoundedBuffer&lt;E&gt; {
 *   <b>final Lock lock = new ReentrantLock();</b>
 *   final Condition notFull  = <b>lock.newCondition(); </b>
 *   final Condition notEmpty = <b>lock.newCondition(); </b>
 *
 *   final Object[] items = new Object[100];
 *   int putptr, takeptr, count;
 *
 *   public void put(E x) throws InterruptedException {
 *     <b>lock.lock();
 *     try {</b>
 *       while (count == items.length)
 *         <b>notFull.await();</b>
 *       items[putptr] = x;
 *       if (++putptr == items.length) putptr = 0;
 *       ++count;
 *       <b>notEmpty.signal();</b>
 *     <b>} finally {
 *       lock.unlock();
 *     }</b>
 *   }
 *
 *   public E take() throws InterruptedException {
 *     <b>lock.lock();
 *     try {</b>
 *       while (count == 0)
 *         <b>notEmpty.await();</b>
 *       E x = (E) items[takeptr];
 *       if (++takeptr == items.length) takeptr = 0;
 *       --count;
 *       <b>notFull.signal();</b>
 *       return x;
 *     <b>} finally {
 *       lock.unlock();
 *     }</b>
 *   }
 * }
 * </pre>
 *
 * (The {@link java.util.concurrent.ArrayBlockingQueue} class provides
 * this functionality, so there is no reason to implement this
 * sample usage class.)
 * ArrayBlockingQueue类实现了这个功能，所以没有理由实现这个示例方法
 *
 * <p>A {@code Condition} implementation can provide behavior and semantics
 * that is
 * different from that of the {@code Object} monitor methods, such as
 * guaranteed ordering for notifications, or not requiring a lock to be held
 * when performing notifications.
 * Condition实现可以提供不同于对象监视器锁方法的行为和语义，例如可以保证通知的顺序，当执行通知时不需要持有锁
 * If an implementation provides such specialized semantics then the
 * implementation must document those semantics.
 * 如果实现提供了这样专门的语义，实现必须文档记录这些语义。
 *
 * <p>Note that {@code Condition} instances are just normal objects and can
 * themselves be used as the target in a {@code synchronized} statement,
 * and can have their own monitor {@link Object#wait wait} and
 * {@link Object#notify notify} methods invoked.
 * Acquiring the monitor lock of a {@code Condition} instance, or using its
 * monitor methods, has no specified relationship with acquiring the
 * {@link Lock} associated with that {@code Condition} or the use of its
 * {@linkplain #await waiting} and {@linkplain #signal signalling} methods.
 * It is recommended that to avoid confusion you never use {@code Condition}
 * instances in this way, except perhaps within their own implementation.
 * 请注意，Condition实例本身是普通对象而且他们本身可以再synchronize语义中当做目标使用，而且可以调用它们自己的监视器wait和notify方法，
 * 获取Condition实例的监视器锁或使用它的监视器方法和获取锁相关联的Condition或使用它们的await方法和singal方法没有特定关系。
 * 以防产生混淆，推荐你不要用这种方式使用Condition实例，除了在他们自己的实现中
 *
 * <p>Except where noted, passing a {@code null} value for any parameter
 * will result in a {@link NullPointerException} being thrown.
 * 除非另有说明，任何参数传入控制将导致抛出空指针。
 *
 * <h2>Implementation Considerations</h2>
 *
 * <p>When waiting upon a {@code Condition}, a &quot;<em>spurious
 * wakeup</em>&quot; is permitted to occur, in
 * general, as a concession to the underlying platform semantics.
 * This has little practical impact on most application programs as a
 * {@code Condition} should always be waited upon in a loop, testing
 * the state predicate that is being waited for.  An implementation is
 * free to remove the possibility of spurious wakeups but it is
 * recommended that applications programmers always assume that they can
 * occur and so always wait in a loop.
 * 当等待条件时，允许发生虚假唤醒，通常情况条，这是对底层平台语义的让步。
 * 这对大多数应用程序没有实际影响，因为Condition通常应该循环等待，测试正在等待的状态谓词。
 * 实现可以自由的移除虚假唤醒的可能性，但是推荐应用程序开发者假设它们会发生，所以总是在循环中等待。
 *
 *
 * <p>The three forms of condition waiting
 * (interruptible, non-interruptible, and timed) may differ in their ease of
 * implementation on some platforms and in their performance characteristics.
 * In particular, it may be difficult to provide these features and maintain
 * specific semantics such as ordering guarantees.
 * Further, the ability to interrupt the actual suspension of the thread may
 * not always be feasible to implement on all platforms.
 * 三种条件等待的形式可能在实现的 难易程度和表现特征上不同在一些平台上。
 * 特别是，提供这些特性和维护这些特殊语义可能很困难，比如保证顺序。
 * 此外，能够中断线程的实际挂起可能不适用于所有实现的平台
 *
 * <p>Consequently, an implementation is not required to define exactly the
 * same guarantees or semantics for all three forms of waiting, nor is it
 * required to support interruption of the actual suspension of the thread.
 * 因此，实现不需要对三种形式的等待精确的定义同样的保证和语义， 也不需要支持实际挂起时的中断。
 *
 * <p>An implementation is required to
 * clearly document the semantics and guarantees provided by each of the
 * waiting methods, and when an implementation does support interruption of
 * thread suspension then it must obey the interruption semantics as defined
 * in this interface.
 *实现需要清楚的记录每个等待方法提供的语义和保证，而且当实现确实支持线程挂起中断时，它必须遵守该接口中定义的中断语义。
 *
 * <p>As interruption generally implies cancellation, and checks for
 * interruption are often infrequent, an implementation can favor responding
 * to an interrupt over normal method return. This is true even if it can be
 * shown that the interrupt occurred after another action that may have
 * unblocked the thread. An implementation should document this behavior.
 * 由于中断通常意味着取消，并且对中断的检查通常不频繁，因此实现可以倾向于响应中断而不是正常的方法返回。
 * 即使可以显示中断发生在可能已经解除线程阻塞的另一个操作之后，也是如此。实现应该记录这种行为。
 *
 * @since 1.5
 * @author Doug Lea
 */
public interface Condition {

    /**
     * Causes the current thread to wait until it is signalled or
     * {@linkplain Thread#interrupt interrupted}.
     * 导致当前线程等待，直到它收到信号或线程中断
     *
     * <p>The lock associated with this {@code Condition} is atomically
     * released and the current thread becomes disabled for thread scheduling
     * purposes and lies dormant until <em>one</em> of four things happens:
     * 与此条件相关联的锁被自动释放，当前线程因线程调度目的而被禁用，并处于休眠状态，直到发生以下四种情况之一:
     *
     * <ul>
     * <li>Some other thread invokes the {@link #signal} method for this
     * {@code Condition} and the current thread happens to be chosen as the
     * thread to be awakened; or
     * 其他线程调用这个条件的signal方法 且 当前线程恰好被选为要唤醒的线程
     * <li>Some other thread invokes the {@link #signalAll} method for this
     * {@code Condition}; or
     * 其他线程调用这个条件的signalAll方法
     * <li>Some other thread {@linkplain Thread#interrupt interrupts} the
     * current thread, and interruption of thread suspension is supported; or
     * 其他线程中断当前线程，且支持线程挂起中断
     * <li>A &quot;<em>spurious wakeup</em>&quot; occurs.
     * 发生虚假唤醒
     * </ul>
     *
     * <p>In all cases, before this method can return the current thread must
     * re-acquire the lock associated with this condition. When the
     * thread returns it is <em>guaranteed</em> to hold this lock.
     * 在所有情况下，在此方法返回之前，当前线程必须重新获取与此条件相关的锁。
     * 当线程返回时，保证它持有此锁。
     *
     * <p>If the current thread:
     * <ul>
     * <li>has its interrupted status set on entry to this method; or
     * <li>is {@linkplain Thread#interrupt interrupted} while waiting
     * and interruption of thread suspension is supported,
     * 如果当前线程在进入此方法时设置了中断状态，或在等待时被中断，且支持线程挂起的中断
     * </ul>
     * then {@link InterruptedException} is thrown and the current thread's
     * interrupted status is cleared. It is not specified, in the first
     * case, whether or not the test for interruption occurs before the lock
     * is released.
     * 然后抛出InterruptedException异常且当前线程的中断状态被清空。
     * 在第一种情况下，没有指定中断测试是否在锁被释放之前发生。
     *
     *
     * <p><b>Implementation Considerations</b>
     *  导入注意事项
     *
     * <p>The current thread is assumed to hold the lock associated with this
     * {@code Condition} when this method is called.
     * It is up to the implementation to determine if this is
     * the case and if not, how to respond. Typically, an exception will be
     * thrown (such as {@link IllegalMonitorStateException}) and the
     * implementation must document that fact.
     * 当调用此方法时，假定当前线程持有与此条件相关联的锁。
     * 这取决于实现来确定情况是否如此，如果不是，如何响应。
     * 通常，会抛出IllegalMonitorStateException异常并且实现必须记录该事实
     *
     *
     * <p>An implementation can favor responding to an interrupt over normal
     * method return in response to a signal. In that case the implementation
     * must ensure that the signal is redirected to another waiting thread, if
     * there is one.
     * 实现可以倾向于响应中断，而不是响应信号的普通方法返回。
     * 在这种情况下，实现必须确保信号被重定向到另一个等待线程（如果有的话）
     *
     * @throws InterruptedException if the current thread is interrupted
     *         (and interruption of thread suspension is supported)
     */
    void await() throws InterruptedException;

    /**
     * Causes the current thread to wait until it is signalled.
     * 导致当前线程等直到它收到信号
     *
     * <p>The lock associated with this condition is atomically
     * released and the current thread becomes disabled for thread scheduling
     * purposes and lies dormant until <em>one</em> of three things happens:
     * 与此条件相关联的锁被自动释放，当前线程因线程调度目的而被禁用，并处于休眠状态，直到发生一下三种情况之一：
     * <ul>
     * <li>Some other thread invokes the {@link #signal} method for this
     * {@code Condition} and the current thread happens to be chosen as the
     * thread to be awakened; or
     * 其他线程调用这个条件的signal方法，且当前线程恰好被选择唤醒
     * <li>Some other thread invokes the {@link #signalAll} method for this
     * {@code Condition}; or
     * 其他线程调用这个条件的signalAll方法
     * <li>A &quot;<em>spurious wakeup</em>&quot; occurs.
     * 发生虚假唤醒
     * </ul>
     *
     * <p>In all cases, before this method can return the current thread must
     * re-acquire the lock associated with this condition. When the
     * thread returns it is <em>guaranteed</em> to hold this lock.
     * 在所有情况下，在方法返回之前当前线程必须重新获取此条件相关联的锁。
     * 当线程返回时，保证他持有这个锁
     *
     * <p>If the current thread's interrupted status is set when it enters
     * this method, or it is {@linkplain Thread#interrupt interrupted}
     * while waiting, it will continue to wait until signalled. When it finally
     * returns from this method its interrupted status will still
     * be set.
     * 如果当前线程在进入此方法时被设置为中断状态，或者在等待时被中断，则它将继续等待直到发出信号。
     * 当它最终从该方法返回时，它的中断状态仍将被设置。
     *
     * <p><b>Implementation Considerations</b>
     * 导入注意事项
     *
     * <p>The current thread is assumed to hold the lock associated with this
     * {@code Condition} when this method is called.
     * It is up to the implementation to determine if this is
     * the case and if not, how to respond. Typically, an exception will be
     * thrown (such as {@link IllegalMonitorStateException}) and the
     * implementation must document that fact.
     * 当此方法被调用时，假定当前线程持有此条件关联的锁。
     * 这取决于实现来确定情况是否如此，如果不是，如何响应。通常，会抛出异常，并且实现必须记录该事件
     *
     */
    void awaitUninterruptibly();

    /**
     * Causes the current thread to wait until it is signalled or interrupted,
     * or the specified waiting time elapses.
     * 导致当前线程等待直到收到信号或被中断，或指定的等待时间已过
     *
     * <p>The lock associated with this condition is atomically
     * released and the current thread becomes disabled for thread scheduling
     * purposes and lies dormant until <em>one</em> of five things happens:
     * 此条件关联的锁将自动释放，当前线程因线程调度目的被禁用且进入休眠状态，直到以下五种情况之一发生
     * <ul>
     * <li>Some other thread invokes the {@link #signal} method for this
     * {@code Condition} and the current thread happens to be chosen as the
     * thread to be awakened; or
     * 其他线程调用此条件的singnal方法，且当前线程恰好被选择唤醒
     * <li>Some other thread invokes the {@link #signalAll} method for this
     * {@code Condition}; or
     * 其他线程调用此条件的signalAll方法
     * <li>Some other thread {@linkplain Thread#interrupt interrupts} the
     * current thread, and interruption of thread suspension is supported; or
     * 其他线程中断当前线程，且支持线程挂起中断
     * <li>The specified waiting time elapses; or
     * 指定的时间已过
     * <li>A &quot;<em>spurious wakeup</em>&quot; occurs.
     * 发生虚假唤醒
     * </ul>
     *
     * <p>In all cases, before this method can return the current thread must
     * re-acquire the lock associated with this condition. When the
     * thread returns it is <em>guaranteed</em> to hold this lock.
     * 在所有情况下，在此方法返回之前 当前线程必须重新获取此条件相关联的锁。
     * 当线程返回时，保证它持有这个锁
     *
     * <p>If the current thread:
     * <ul>
     * <li>has its interrupted status set on entry to this method; or
     * <li>is {@linkplain Thread#interrupt interrupted} while waiting
     * and interruption of thread suspension is supported,
     * 如果当前线程在进入此方法时设置了中断状态，或在等待时被中断，支持线程挂起的中断。
     *
     * </ul>
     * then {@link InterruptedException} is thrown and the current thread's
     * interrupted status is cleared. It is not specified, in the first
     * case, whether or not the test for interruption occurs before the lock
     * is released.
     * 然后InterruptedException异常被抛出，当前线程的中断状态被清除。
     * 在第一种情况下，没有指定中断测试是否在锁被释放之前发生。
     *
     * <p>The method returns an estimate of the number of nanoseconds
     * remaining to wait given the supplied {@code nanosTimeout}
     * value upon return, or a value less than or equal to zero if it
     * timed out. This value can be used to determine whether and how
     * long to re-wait in cases where the wait returns but an awaited
     * condition still does not hold. Typical uses of this method take
     * the following form:
     * 在给定返回时提供的nanosTimeout值的情况下，该方法返回剩余等待纳秒数的估计值，如果超时，则返回小于或等于零的值。
     * 在等待条件返回但是等待条件任然不成立的情况下，此值可以用于确定是否重新等待以及等待多次时间。
     * 该方法的典型用法如下
     *
     * <pre> {@code
     * boolean aMethod(long timeout, TimeUnit unit)
     *     throws InterruptedException {
     *   long nanosRemaining = unit.toNanos(timeout);
     *   lock.lock();
     *   try {
     *     while (!conditionBeingWaitedFor()) {
     *       if (nanosRemaining <= 0L)
     *         return false;
     *       nanosRemaining = theCondition.awaitNanos(nanosRemaining);
     *     }
     *     // ...
     *     return true;
     *   } finally {
     *     lock.unlock();
     *   }
     * }}</pre>
     *
     * <p>Design note: This method requires a nanosecond argument so
     * as to avoid truncation errors in reporting remaining times.
     * Such precision loss would make it difficult for programmers to
     * ensure that total waiting times are not systematically shorter
     * than specified when re-waits occur.
     * 设计说明：此方法需要一个纳秒参数，以避免在报告剩余时间时出现截断错误。
     * 这种精度损失将使程序员难以确保在发生重新等待时总等待时间不会系统地短于指定的时间
     *
     * <p><b>Implementation Considerations</b>
     * 实现注意事项
     *
     * <p>The current thread is assumed to hold the lock associated with this
     * {@code Condition} when this method is called.
     * It is up to the implementation to determine if this is
     * the case and if not, how to respond. Typically, an exception will be
     * thrown (such as {@link IllegalMonitorStateException}) and the
     * implementation must document that fact.
     * 当调用此方法时，当前线程假定持有该条件关联的锁。
     * 这取决于实现来决定情况是否如此，如果不是，如何响应。
     * 通常会抛出IllegalMonitorStateException异常且实现必须记录该事实
     *
     * <p>An implementation can favor responding to an interrupt over normal
     * method return in response to a signal, or over indicating the elapse
     * of the specified waiting time. In either case the implementation
     * must ensure that the signal is redirected to another waiting thread, if
     * there is one.
     * 实现可以倾向于响应中断而不是响应信号的普通方法返回，或者不是指示等待时间的流逝。
     * 无论在哪种情况下，实现都必须确保信号被重定向到另一个等待线程（如果有的话）
     *
     * @param nanosTimeout the maximum time to wait, in nanoseconds
     * @return an estimate of the {@code nanosTimeout} value minus
     *         the time spent waiting upon return from this method.
     *         A positive value may be used as the argument to a
     *         subsequent call to this method to finish waiting out
     *         the desired time.  A value less than or equal to zero
     *         indicates that no time remains.
     * @throws InterruptedException if the current thread is interrupted
     *         (and interruption of thread suspension is supported)
     */
    long awaitNanos(long nanosTimeout) throws InterruptedException;

    /**
     * Causes the current thread to wait until it is signalled or interrupted,
     * or the specified waiting time elapses. This method is behaviorally
     * equivalent to:
     * <pre> {@code awaitNanos(unit.toNanos(time)) > 0}</pre>
     * 导致当前线程等待直到被信号通知或被中断或过了指定等待时间。
     * 该方法行为等价于awaitNanos(unit.toNanos(time)) > 0
     *
     * @param time the maximum time to wait
     * @param unit the time unit of the {@code time} argument
     * @return {@code false} if the waiting time detectably elapsed
     *         before return from the method, else {@code true}
     * @throws InterruptedException if the current thread is interrupted
     *         (and interruption of thread suspension is supported)
     */
    boolean await(long time, TimeUnit unit) throws InterruptedException;

    /**
     * Causes the current thread to wait until it is signalled or interrupted,
     * or the specified deadline elapses.
     * 导致当前线程等待直到收到信号通知或被中断或指定截止时间已过
     *
     * <p>The lock associated with this condition is atomically
     * released and the current thread becomes disabled for thread scheduling
     * purposes and lies dormant until <em>one</em> of five things happens:
     * 与此条件相关联的锁被自动释放，当前线程因线程调度目睹而被禁用并处于休眠状态，直到发生以下五种情况之一
     * <ul>
     * <li>Some other thread invokes the {@link #signal} method for this
     * {@code Condition} and the current thread happens to be chosen as the
     * thread to be awakened; or
     * 其他线程调用此条件的signal方法，且当前线程恰好被选择唤醒
     * <li>Some other thread invokes the {@link #signalAll} method for this
     * {@code Condition}; or
     * 其他线程调用此条件的signalAll方法
     * <li>Some other thread {@linkplain Thread#interrupt interrupts} the
     * current thread, and interruption of thread suspension is supported; or
     * <li>The specified deadline elapses; or
     * 其他线程中断当前线程，且这支持线程挂起中断，或过了指定的结束时间
     * <li>A &quot;<em>spurious wakeup</em>&quot; occurs.
     * 发生虚假唤醒
     * </ul>
     *
     * <p>In all cases, before this method can return the current thread must
     * re-acquire the lock associated with this condition. When the
     * thread returns it is <em>guaranteed</em> to hold this lock.
     * 在所有情况下，在此方法返回之前当前线程必须重新获取此条件相关联的锁。
     * 当线程返回时必须保证持有这个锁
     *
     * <p>If the current thread:
     * <ul>
     * <li>has its interrupted status set on entry to this method; or
     * <li>is {@linkplain Thread#interrupt interrupted} while waiting
     * and interruption of thread suspension is supported,
     * 如果当前线程在进入此方法时设置了中断状态
     * 或在等待时被中断，且支持线程挂起的中断
     *
     * </ul>
     * then {@link InterruptedException} is thrown and the current thread's
     * interrupted status is cleared. It is not specified, in the first
     * case, whether or not the test for interruption occurs before the lock
     * is released.
     * 然后抛出InterruptedException 且当前线程的中断状态被清除。
     * 在第一种情况下，没有指定中断测试是否在锁被释放之前发生
     *
     * <p>The return value indicates whether the deadline has elapsed,
     * which can be used as follows:
     * 返回值表名是否已经过了过期时间，可以按以下用法
     * <pre> {@code
     * boolean aMethod(Date deadline)
     *     throws InterruptedException {
     *   boolean stillWaiting = true;
     *   lock.lock();
     *   try {
     *     while (!conditionBeingWaitedFor()) {
     *       if (!stillWaiting)
     *         return false;
     *       stillWaiting = theCondition.awaitUntil(deadline);
     *     }
     *     // ...
     *     return true;
     *   } finally {
     *     lock.unlock();
     *   }
     * }}</pre>
     *
     * <p><b>Implementation Considerations</b>
     * 实现注意
     *
     * <p>The current thread is assumed to hold the lock associated with this
     * {@code Condition} when this method is called.
     * It is up to the implementation to determine if this is
     * the case and if not, how to respond. Typically, an exception will be
     * thrown (such as {@link IllegalMonitorStateException}) and the
     * implementation must document that fact.
     * 当该方法被调用时当前线程假定持有该条件关联的锁。
     * 这种情况取决于实现来决定，如果不是如何响应。
     * 通常将抛出IllegalMonitorStateException异常且实现必须记录该事实
     *
     * <p>An implementation can favor responding to an interrupt over normal
     * method return in response to a signal, or over indicating the passing
     * of the specified deadline. In either case the implementation
     * must ensure that the signal is redirected to another waiting thread, if
     * there is one.
     * 实现倾向于返回中断而不是响应信号的正常方法返回，或优于指定截止日期的通过。
     * 在任何一种情况下，实现都必须确保信号被重定向到另一个等待线程
     *
     * @param deadline the absolute time to wait until
     * @return {@code false} if the deadline has elapsed upon return, else
     *         {@code true}
     * @throws InterruptedException if the current thread is interrupted
     *         (and interruption of thread suspension is supported)
     */
    boolean awaitUntil(Date deadline) throws InterruptedException;

    /**
     * Wakes up one waiting thread.
     * 唤醒一个等待的线程
     *
     * <p>If any threads are waiting on this condition then one
     * is selected for waking up. That thread must then re-acquire the
     * lock before returning from {@code await}.
     * 如果有线程等待此条件，则选择一个线程进行唤醒。该线程必须在await返回之前重新获取锁
     *
     * <p><b>Implementation Considerations</b>
     * 实现注意点
     *
     * <p>An implementation may (and typically does) require that the
     * current thread hold the lock associated with this {@code
     * Condition} when this method is called. Implementations must
     * document this precondition and any actions taken if the lock is
     * not held. Typically, an exception such as {@link
     * IllegalMonitorStateException} will be thrown.
     * 当此方法被调用时，实现需要当前线程持有该条件相关联的锁。
     * 实现必须记录先决条件以及未持有锁时所采取的任何操作。通常，会抛出一个异常，比如IllegalMonitorStateException
     */
    void signal();

    /**
     * Wakes up all waiting threads.
     * 唤醒所有等待的线程
     *
     * <p>If any threads are waiting on this condition then they are
     * all woken up. Each thread must re-acquire the lock before it can
     * return from {@code await}.
     * 如果有任何线程等待该条件，他们都将唤醒。
     * 每个线程必须在await返回之前重新获取锁。
     *
     * <p><b>Implementation Considerations</b>
     *
     * <p>An implementation may (and typically does) require that the
     * current thread hold the lock associated with this {@code
     * Condition} when this method is called. Implementations must
     * document this precondition and any actions taken if the lock is
     * not held. Typically, an exception such as {@link
     * IllegalMonitorStateException} will be thrown.
     * 当该方法被调用时。实现可能需要当前线程持有该条件相关联的锁。
     * 实现必须记录先决条件和任何锁未被持有时采取的动作。通常会抛出IllegalMonitorStateException
     */
    void signalAll();
}
