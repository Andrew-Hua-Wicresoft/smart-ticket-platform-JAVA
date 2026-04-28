import { useRef, useCallback, useEffect } from 'react';

export function useDebouncedCallback<TArgs extends unknown[]>(
  callback: (...args: TArgs) => void | Promise<void>,
  delay: number
): [(...args: TArgs) => void, () => void] {
  const timeoutRef = useRef<ReturnType<typeof setTimeout> | null>(null);
  const callbackRef = useRef(callback);

  useEffect(() => {
    callbackRef.current = callback;
  }, [callback]);

  useEffect(() => {
    return () => {
      if (timeoutRef.current) clearTimeout(timeoutRef.current);
    };
  }, []);

  const debounced = useCallback(
    (...args: TArgs) => {
      if (timeoutRef.current) clearTimeout(timeoutRef.current);
      timeoutRef.current = setTimeout(() => callbackRef.current(...args), delay);
    },
    [delay]
  );

  const cancel = useCallback(() => {
    if (timeoutRef.current) clearTimeout(timeoutRef.current);
  }, []);

  return [debounced, cancel];
}
